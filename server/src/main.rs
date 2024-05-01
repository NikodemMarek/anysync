use std::{net::SocketAddr, path::PathBuf};
use axum::{extract::{Json, State}, http::StatusCode, routing::{get, Router}};
use tracing_subscriber::{layer::SubscriberExt, util::SubscriberInitExt};
use tower_http::trace::{DefaultMakeSpan, TraceLayer};
use clap::Parser;

mod send_file;
mod get_file;
mod paths;
mod file_stream;

use send_file::send_file_handler;

#[derive(Parser, Debug)]
#[command(version, about, long_about = None)]
struct Args {
    #[arg(short, long, default_value = "../testdir")]
    root_path: PathBuf,

    #[arg(short, long, default_value = "5060")]
    port: u16,
}

#[derive(Debug, Clone)]
struct AppState {
    root_path: PathBuf,
}

async fn get_paths(State(AppState { root_path }): State<AppState>) -> (StatusCode, Json<Vec<String>>) {
    let files = paths::get_files_relative(&root_path).unwrap();
    (StatusCode::OK, Json(files.iter().map(|p| p.to_string_lossy().to_string()).collect()))
}

#[tokio::main]
async fn main() {
    let Args { root_path, port } = Args::parse();

    let fl = get_file::get_missing_local_files(&root_path).await;
    println!("{:?}", fl);
    let r = get_file::get_file(&root_path, "test1.txt").await;
    println!("{:?}", r);

    let state = AppState { root_path };

    tracing_subscriber::registry()
        .with(
            tracing_subscriber::EnvFilter::try_from_default_env()
                .unwrap_or_else(|_| "example_websockets=debug,tower_http=debug".into()),
        )
        .with(tracing_subscriber::fmt::layer())
        .init();

    let app = Router::new()
        .route("/paths", get(get_paths))
        .route("/get/:path", get(send_file_handler))
        .layer(
            TraceLayer::new_for_http()
                .make_span_with(DefaultMakeSpan::default().include_headers(true)),
        )
        .with_state(state);

    let listener = tokio::net::TcpListener::bind(format!("0.0.0.0:{}", port)).await.unwrap();
    axum::serve(listener, app.into_make_service_with_connect_info::<SocketAddr>()).await.unwrap();
}
