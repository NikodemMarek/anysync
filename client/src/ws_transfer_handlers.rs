use std::path::PathBuf;
use futures_util::StreamExt;
use tokio_tungstenite::connect_async;
use eyre::Result;

const SERVER_ADDR: &str = "localhost:5060";

pub async fn get_file(root_path: &PathBuf, path: &PathBuf) -> Result<()> {
    let url = format!("ws://{}/get?path={}", SERVER_ADDR, path.to_string_lossy().to_string());
    let local_path = root_path.join(path);

    let (ws_stream, _) = connect_async(url).await?;
    let (_, read) = ws_stream.split();

    tokio::spawn(async move {
        let res = lib::file_stream::stream_to_file(local_path, read).await;
        if let Err(e) = res {
            eprintln!("error while receiving file: {}", e);
        }
    });

    Ok(())
}

pub async fn set_file(root_path: &PathBuf, path: &PathBuf) -> Result<()> {
    let url = format!("ws://{}/set?path={}", SERVER_ADDR, path.to_string_lossy().to_string());
    let local_path = root_path.join(path);

    let (ws_stream, _) = connect_async(url).await?;
    let (write, _) = ws_stream.split();

    tokio::spawn(async move {
        let res = lib::file_stream::stream_from_file(local_path, write, None).await;
        if let Err(e) = res {
            eprintln!("error while receiving file: {}", e);
        }
    });

    Ok(())
}
