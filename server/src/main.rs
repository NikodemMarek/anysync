use eyre::{Ok, Result};
use std::{fs, net::SocketAddr, path::PathBuf};
use axum::{extract::{Json, Path, State}, http::StatusCode, routing::{get, Router}};
use tracing_subscriber::{layer::SubscriberExt, util::SubscriberInitExt};
use tower_http::trace::{DefaultMakeSpan, TraceLayer};
use clap::Parser;

mod stream_file;

use stream_file::stream_file_handler;

#[derive(Debug, Clone)]
struct AppState {
    root_path: PathBuf,
}

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

async fn get_abs_paths(State(AppState { root_path }): State<AppState>) -> (StatusCode, Json<Vec<String>>) {
    let files = get_files(&root_path).unwrap();
    (StatusCode::OK, Json(files.iter().map(|p| p.strip_prefix(&root_path).unwrap().to_str().unwrap().to_string()).collect()))
}
async fn get_paths(State(AppState { root_path }): State<AppState>, Path(path): Path<String>) -> (StatusCode, Json<Vec<String>>) {
    let root_path = root_path.join(path);

    let files = get_files(&root_path).unwrap();
    (StatusCode::OK, Json(files.iter().map(|p| p.strip_prefix(&root_path).unwrap().to_str().unwrap().to_string()).collect()))
}

#[derive(Parser, Debug)]
#[command(version, about, long_about = None)]
struct Args {
    #[arg(short, long, default_value = "../testdir")]
    root_path: PathBuf,

    #[arg(short, long, default_value = "5060")]
    port: u16,
}

#[tokio::main]
async fn main() {
    let Args { root_path, port } = Args::parse();
    let state = AppState { root_path };

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
        .route("/:path", get(stream_file_handler))
        .layer(
            TraceLayer::new_for_http()
                .make_span_with(DefaultMakeSpan::default().include_headers(true)),
        )
        .with_state(state);

    let listener = tokio::net::TcpListener::bind(format!("0.0.0.0:{}", port)).await.unwrap();
    axum::serve(listener, app.into_make_service_with_connect_info::<SocketAddr>()).await.unwrap();
}
