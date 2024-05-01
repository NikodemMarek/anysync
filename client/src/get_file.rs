use std::path::PathBuf;
use futures_util::StreamExt;
use tokio_tungstenite::connect_async;
use eyre::Result;
use reqwest;

const SERVER_ADDR: &str = "localhost:5060";

async fn get_remote_files() -> Result<Vec<PathBuf>> {
    let url = format!("http://{}/paths", SERVER_ADDR);

    let res = reqwest::get(url).await?
        .json::<Vec<String>>()
        .await?
        .iter()
        .map(|s| PathBuf::from(s))
        .collect();

    Ok(res)
}

pub async fn get_missing_local_files(root_path: &PathBuf) -> Result<Vec<PathBuf>> {
    let local_files = lib::paths::get_files_relative(&root_path)?;
    let remote_files = get_remote_files().await?;

    let missing = lib::paths::missing(&local_files, &remote_files);

    Ok(missing)
}

pub async fn get_file(root_path: &PathBuf, path: &str) -> Result<()> {
    let url = format!("ws://{}/get/{}", SERVER_ADDR, path);
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

pub async fn set_file(root_path: &PathBuf, path: &str) -> Result<()> {
    let url = format!("ws://{}/set/{}", SERVER_ADDR, path);
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
