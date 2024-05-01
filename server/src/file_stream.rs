use std::{borrow::Cow, fs::{self, OpenOptions}, io::{Read, Write}, path::PathBuf};
use futures::{stream::StreamExt, SinkExt};

pub async fn stream_to_file(
    path: PathBuf,
    mut stream: impl StreamExt<Item = tokio_tungstenite::tungstenite::Result<tokio_tungstenite::tungstenite::Message>> + std::marker::Unpin
) -> eyre::Result<()> {
    use tokio_tungstenite::tungstenite::Message;

    if path.exists() {
        if path.is_dir() {
            return Err(eyre::eyre!("path {} exists and is a directory", path.display()));
        } else {
            fs::remove_file(&path).unwrap();
        }
    }

    let mut file = OpenOptions::new()
        .create(true)
        .append(true)
        .open(&path)?;

    while let Some(Ok(message)) = stream.next().await {
        match message {
            Message::Binary(bin) => {
                file.write_all(&bin)?;
            }
            Message::Close(reason) => {
                if let Some(reason) = reason {
                    if reason.reason == "EOF" {
                        return Ok(());
                    } else {
                        return Err(eyre::eyre!("received close message with reason: {}", reason.reason));
                    }
                } else {
                    return Err(eyre::eyre!("received close message without reason"));
                }
            }
            _ => return Err(eyre::eyre!("received unexpected message type"))
        }
    }

    Err(eyre::eyre!("stream ended without close message"))
}

pub async fn stream_from_file(
    path: PathBuf,
    mut stream: impl SinkExt<axum::extract::ws::Message> + std::marker::Unpin,
    chunk_size: usize
) -> eyre::Result<()> {
    use axum::extract::ws::{CloseFrame, Message};

    let mut file = OpenOptions::new()
        .read(true)
        .open(&path)?;

    loop {
        let mut chunk = Vec::with_capacity(chunk_size);
        let n = Write::by_ref(&mut file)
            .take(chunk_size as u64)
            .read_to_end(&mut chunk)?;
        if n == 0 { break; }

        let _ = stream
            .send(Message::Binary(chunk))
            .await;

        if n < chunk_size { break; }
    }

    let close_msg = CloseFrame {
        code: axum::extract::ws::close_code::NORMAL,
        reason: Cow::Borrowed("EOF"),
    };
    stream
        .send(Message::Close(Some(close_msg))).await
        .map_err(|_| eyre::eyre!("error while sending close frame"))?;

    Ok(())
}
