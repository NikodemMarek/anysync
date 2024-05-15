use axum::{
    extract::{ws::WebSocketUpgrade, Path}, http::{Response, StatusCode}, response::IntoResponse
};
use futures::stream::StreamExt;

use crate::{config::Actions, CONFIG};

pub async fn get_file_handler(
    ws: WebSocketUpgrade,

    Path((source, path)): Path<(String, String)>,
) -> impl IntoResponse {
    let source_config = CONFIG.sources.get(&source).expect("source not found");

    match source_config.actions {
        Actions::None | Actions::Set => return Response::builder()
            .status(StatusCode::FORBIDDEN)
            .body("method not allowed".into())
            .unwrap(),
        Actions::Get | Actions::GetSet => {}
    }

    let path = source_config.path.join(path);
    let chunk_size = None;

    ws.on_upgrade(move |socket| async move {
        let (sender, _) = socket.split();

        tokio::spawn(async move {
            let res = lib::file_stream::stream_from_file(path, sender, chunk_size).await;
            if let Err(e) = res {
                eprintln!("error while streaming file: {}", e);
            }
        });
    })
}

pub async fn set_file_handler(
    ws: WebSocketUpgrade,

    Path((source, path)): Path<(String, String)>,
) -> impl IntoResponse {
    let source_config = CONFIG.sources.get(&source).expect("source not found");

    match source_config.actions {
        Actions::None | Actions::Get => return Response::builder()
            .status(StatusCode::FORBIDDEN)
            .body("method not allowed".into())
            .unwrap(),
        Actions::Set | Actions::GetSet => {}
    }

    let path = source_config.path.join(path);

    ws.on_upgrade(move |socket| async {
        let (_, reciever) = socket.split();

        tokio::spawn(async move {
            let res = lib::file_stream::stream_to_file(path, reciever).await;
            if let Err(e) = res {
                eprintln!("error while streaming file: {}", e);
            }
        });
    })
}
