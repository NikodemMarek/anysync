use axum::{
    extract::{ws::{WebSocket, WebSocketUpgrade}, Path, Query, State},
    response::IntoResponse,
};
use std::{collections::HashMap, path::PathBuf};
use futures::stream::StreamExt;

use crate::AppState;

pub async fn send_file_handler(
    State(AppState { root_path }): State<AppState>,
    ws: WebSocketUpgrade,

    Path(path): Path<String>,
    Query(params): Query<HashMap<String, String>>,
) -> impl IntoResponse {
    let file = root_path.join(path);
    let chunk_size = params.get("chunk_size").and_then(|s| s.parse().ok()).unwrap_or(0x4000);

    ws.on_upgrade(move |socket| stream_file(socket, file, chunk_size))
}

async fn stream_file(socket: WebSocket, path: PathBuf, chunk_size: usize) {
    let (sender, _) = socket.split();

    tokio::spawn(async move {
        let res = crate::file_stream::stream_from_file(path, sender, chunk_size).await;
        if let Err(e) = res {
            eprintln!("error while streaming file: {}", e);
        }
    });
}
