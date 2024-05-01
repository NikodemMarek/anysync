use axum::{
    extract::{ws::WebSocketUpgrade, Path, Query, State},
    response::IntoResponse,
}; use std::collections::HashMap;
use futures::stream::StreamExt;

use crate::AppState;

pub async fn get_file_handler(
    State(AppState { root_path }): State<AppState>,
    ws: WebSocketUpgrade,

    Path(path): Path<String>,
    Query(params): Query<HashMap<String, String>>,
) -> impl IntoResponse {
    let path = root_path.join(path);
    let chunk_size = params.get("chunk_size").and_then(|s| s.parse().ok());

    ws.on_upgrade(move |socket| async move {
        let (sender, _) = socket.split();

        tokio::spawn(async move {
            let res = crate::file_stream::stream_from_file(path, sender, chunk_size).await;
            if let Err(e) = res {
                eprintln!("error while streaming file: {}", e);
            }
        });
    })
}

pub async fn set_file_handler(
    State(AppState { root_path }): State<AppState>,
    ws: WebSocketUpgrade,

    Path(path): Path<String>,
) -> impl IntoResponse {
    let path = root_path.join(path);

    ws.on_upgrade(move |socket| async {
        let (_, reciever) = socket.split();

        tokio::spawn(async move {
            let res = crate::file_stream::stream_to_file(path, reciever).await;
            if let Err(e) = res {
                eprintln!("error while streaming file: {}", e);
            }
        });
    })
}
