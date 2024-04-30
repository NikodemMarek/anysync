use std::{fs::{self, OpenOptions}, io::Write, path::PathBuf};
use futures_util::StreamExt;
use tokio_tungstenite::{connect_async, tungstenite::protocol::Message};
use eyre::Result;
use reqwest;

const SERVER_ADDR: &str = "localhost:5060";

async fn get_remote_files() -> Result<Vec<PathBuf>> {
    let url = url::Url::parse(&format!("http://{}", SERVER_ADDR))?.join("paths")?;

    let res = reqwest::get(url).await?
        .json::<Vec<String>>()
        .await?
        .iter()
        .map(|s| PathBuf::from(s))
        .collect();

    Ok(res)
}

pub async fn get_missing_local_files(root_path: &PathBuf) -> Result<Vec<String>> {
    let local_files = crate::paths::get_files_relative(&root_path)?;
    let remote_files = get_remote_files().await?;

    let diff = remote_files.into_iter()
        .filter(|remote_file| !local_files.iter().any(|local_file| local_file == remote_file))
        .map(|path| path.to_string_lossy().to_string())
        .collect();

    Ok(diff)
}

pub async fn get_file(root_path: &PathBuf, path: &str) -> Result<()> {
    let url = url::Url::parse(&format!("ws://{}", SERVER_ADDR))?.join(format!("get/{}", path).as_str())?;
    let local_path = root_path.join(path);

    let (mut ws_stream, _) = connect_async(url).await?;

    tokio::spawn(async move {
        if local_path.exists() {
            if local_path.is_dir() {
                println!("path {} exists and is a directory", local_path.display());
                return;
            } else {
                fs::remove_file(&local_path).unwrap();
            }
        }

        let mut file = OpenOptions::new()
            .create(true)
            .append(true)
            .open(&local_path)
            .unwrap();


        while let Some(Ok(message)) = ws_stream.next().await {
            match message {
                Message::Binary(bin) => {
                    file.write_all(&bin).unwrap();
                }
                Message::Close(reason) => {
                    if reason.unwrap().reason == "EOF" {
                        println!("file received successfully");
                    } else {
                        println!("file transfer failed");
                    }

                    break;
                }
                _ => {
                    println!("Received a non-binary message: {:?}", message);
                }
            }
        }
    });

    Ok(())
}
