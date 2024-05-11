use std::net::SocketAddr;
use axum::{extract::{Json, Path}, http::StatusCode, routing::{get, Router}};
use tracing_subscriber::{layer::SubscriberExt, util::SubscriberInitExt};
use tower_http::trace::{DefaultMakeSpan, TraceLayer};
use lazy_static::lazy_static;

mod ws_transfer_handlers;
mod config;

lazy_static! {
    static ref CONFIG: config::Config = config::get_final()
        .expect("failed to get config");
}

async fn get_paths(
    Path(source): Path<String>,
) -> (StatusCode, Json<Vec<String>>) {
    let source_config = CONFIG.sources.get(&source).expect("source not found");

    let files = lib::paths::get_files_relative(&source_config.dir).unwrap();
    (StatusCode::OK, Json(files.iter().map(|p| p.to_string_lossy().to_string()).collect()))
}

#[tokio::main]
async fn main() {
    tracing_subscriber::registry()
        .with(
            tracing_subscriber::EnvFilter::try_from_default_env()
                .unwrap_or_else(|_| "example_websockets=debug,tower_http=debug".into()),
        )
        .with(tracing_subscriber::fmt::layer())
        .init();

    let app = Router::new()
        .route("/:source/paths", get(get_paths))
        .route("/:source/get/*path", get(ws_transfer_handlers::get_file_handler))
        .route("/:source/set/*path", get(ws_transfer_handlers::set_file_handler))
        .layer(
            TraceLayer::new_for_http()
                .make_span_with(DefaultMakeSpan::default().include_headers(true)),
        );

    let listener = tokio::net::TcpListener::bind(format!("0.0.0.0:{}", &CONFIG.port)).await.unwrap();
    axum::serve(listener, app.into_make_service_with_connect_info::<SocketAddr>()).await.unwrap();
}
