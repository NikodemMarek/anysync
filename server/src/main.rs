use eyre::{Ok, Result};
use std::{fs, net::SocketAddr, path::PathBuf};
use axum::{extract::{Json, Path}, http::StatusCode, routing::{get, Router}};
use tracing_subscriber::{layer::SubscriberExt, util::SubscriberInitExt};
use tower_http::{services::ServeDir, trace::{DefaultMakeSpan, TraceLayer}};

mod ws;

use ws::ws_handler;

const ROOT_PATH: &str = "../testdir";

fn get_files(path: &PathBuf) -> Result<Vec<PathBuf>> {
    let files = fs::read_dir(path)?
        .try_fold(Vec::new(), |mut acc, entry| {
            let path = entry?.path();

            if path.is_dir() {
                acc.extend(get_files(&path)?);
                return Ok(acc);
            }

            acc.push(path);
            Ok(acc)
        })?;

    Ok(files)
}

async fn get_abs_paths() -> (StatusCode, Json<Vec<String>>) {
    let root_path = PathBuf::from(ROOT_PATH);

    let files = get_files(&root_path).unwrap();
    (StatusCode::OK, Json(files.iter().map(|p| p.strip_prefix(&root_path).unwrap().to_str().unwrap().to_string()).collect()))
}
async fn get_paths(Path(path): Path<String>) -> (StatusCode, Json<Vec<String>>) {
    let root_path = PathBuf::from(ROOT_PATH).join(path.as_str());

    let files = get_files(&root_path).unwrap();
    (StatusCode::OK, Json(files.iter().map(|p| p.strip_prefix(&root_path).unwrap().to_str().unwrap().to_string()).collect()))
}

#[tokio::main]
async fn main() {
    let root_path = PathBuf::from(ROOT_PATH);
    let files = get_files(&root_path).unwrap();

    tracing_subscriber::registry()
        .with(
            tracing_subscriber::EnvFilter::try_from_default_env()
                .unwrap_or_else(|_| "example_websockets=debug,tower_http=debug".into()),
        )
        .with(tracing_subscriber::fmt::layer())
        .init();

    let app = Router::new()
        .route("/paths", get(get_abs_paths))
        .route("/paths/:path", get(get_paths))
        .nest_service("/data", ServeDir::new(&root_path.clone()))
        .route("/:file", get(ws_handler))
        .layer(
            TraceLayer::new_for_http()
                .make_span_with(DefaultMakeSpan::default().include_headers(true)),
        );

    let listener = tokio::net::TcpListener::bind("0.0.0.0:5060").await.unwrap();
    axum::serve(listener, app.into_make_service_with_connect_info::<SocketAddr>()).await.unwrap();

    dbg!(files);
}
