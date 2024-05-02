use std::path::PathBuf;
use futures_util::StreamExt;
use tokio_tungstenite::connect_async;
use eyre::Result;

use crate::CONFIG;

pub async fn get_file(root_path: &PathBuf, path: &PathBuf) -> Result<()> {
    let url = &CONFIG.get_url(&path.to_string_lossy());
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
    let url = &CONFIG.set_url(&path.to_string_lossy());
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
