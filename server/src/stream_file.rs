use axum::{
    extract::{ws::{Message, WebSocket, WebSocketUpgrade}, Path, Query},
    response::IntoResponse,
};
use std::{borrow::Cow, collections::HashMap, fs::File, io::Write, path::PathBuf};
use axum::extract::ws::CloseFrame;
use futures::{sink::SinkExt, stream::StreamExt};
use eyre::Result;
use std::io::Read;

pub async fn stream_file_handler(
    ws: WebSocketUpgrade,

    Path(file): Path<String>,
    Query(params): Query<HashMap<String, String>>,
) -> impl IntoResponse {
    let file = PathBuf::from(format!("../testdir/{}", file));
    let chunk_size = params.get("chunk_size").and_then(|s| s.parse().ok()).unwrap_or(0x4000);

    if let Ok(file) = std::fs::File::open(file) {
        ws.on_upgrade(move |socket| stream_file(socket, file, chunk_size))
    } else {
        ws.on_upgrade(|_| async { () })
    }
}

async fn stream_file(socket: WebSocket, mut file: File, chunk_size: usize) {
    let (mut sender, _) = socket.split();

    let mut send_task: tokio::task::JoinHandle<Result<_>> = tokio::spawn(async move {
        loop {
            let mut chunk = Vec::with_capacity(chunk_size);
            let n = Write::by_ref(&mut file)
                .take(chunk_size as u64)
                .read_to_end(&mut chunk)?;
            if n == 0 { break; }

            sender
                .send(Message::Binary(chunk))
                .await?;

            if n < chunk_size { break; }
        }

        sender
            .send(Message::Close(Some(CloseFrame {
                code: axum::extract::ws::close_code::NORMAL,
                reason: Cow::Borrowed("EOF"),
            })))
            .await?;

        return Ok(());
    });

    tokio::select! {
        rv_a = (&mut send_task) => {
            match rv_a {
                Ok(a) => {
                    match a {
                        Ok(_) => println!("file sent successfully"),
                        Err(e) => println!("failed to send file: {e}")
                    };
                }
                Err(a) => println!("error sending file: {a:?}")
            }
        },
    }
}
