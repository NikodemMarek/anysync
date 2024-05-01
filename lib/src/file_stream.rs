use std::{borrow::Cow, fs::{self, OpenOptions}, io::{Read, Write}, path::PathBuf};
use futures::{stream::StreamExt, SinkExt};

use crate::msg::Msg;

pub async fn stream_to_file<M, E>(
    path: PathBuf,
    mut stream: impl StreamExt<Item = Result<M, E>> + std::marker::Unpin
) -> eyre::Result<()>
where
    M: Into<Msg>,
    E: std::error::Error,
{
    if path.exists() {
        if path.is_dir() {
            return Err(eyre::eyre!("path {} exists and is a directory", path.display()));
        } else {
            fs::remove_file(&path).unwrap();
        }
    } else {
        let parent = path.parent().unwrap();
        if !parent.exists() {
            fs::create_dir_all(parent)?;
        }
    }

    let mut file = OpenOptions::new()
        .create(true)
        .append(true)
        .open(&path)?;

    while let Some(Ok(message)) = stream.next().await {
        match message.into() {
            Msg::Binary(bin) => {
                file.write_all(&bin)?;
            }
            Msg::Close(reason) => {
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

pub async fn stream_from_file<'a, M>(
    path: PathBuf,
    mut stream: impl SinkExt<M> + std::marker::Unpin,
    chunk_size: Option<usize>,
) -> eyre::Result<()>
where
    M: From<Msg>,
{
    let chunk_size = chunk_size.unwrap_or(1024);
    let mut file = OpenOptions::new()
        .read(true)
        .open(&path)?;

    loop {
        let mut chunk = Vec::with_capacity(chunk_size);
        let n = Write::by_ref(&mut file)
            .take(chunk_size as u64)
            .read_to_end(&mut chunk)?;
        if n == 0 { break; }

        stream
            .send(Msg::Binary(chunk).into())
            .await
            .map_err(|_| eyre::eyre!("error while sending binary message"))?;

        if n < chunk_size { break; }
    }

    let close_msg = Some(tokio_tungstenite::tungstenite::protocol::CloseFrame {
        code: tokio_tungstenite::tungstenite::protocol::frame::coding::CloseCode::Normal,
        reason: Cow::Borrowed("EOF"),
    });
    stream
        .send(Msg::Close(close_msg).into()).await
        .map_err(|_| eyre::eyre!("error while sending close frame"))?;

    Ok(())
}
