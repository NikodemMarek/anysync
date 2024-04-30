use std::{env, path::PathBuf};
use futures_util::{future, pin_mut, StreamExt};
use tokio::io::{AsyncReadExt, AsyncWriteExt};
use tokio_tungstenite::{connect_async, tungstenite::protocol::Message};
use eyre::{Ok, Result};
use reqwest;

const SERVER_ADDR: &str = "http://localhost:5060";

async fn get_remote_files() -> Result<Vec<PathBuf>> {
    let url = url::Url::parse(SERVER_ADDR)?.join("paths")?;

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

    println!("{:?}", local_files);
    println!("{:?}", remote_files);

    let diff = remote_files.into_iter()
        .filter(|remote_file| !local_files.iter().any(|local_file| local_file == remote_file))
        .map(|path| path.to_string_lossy().to_string())
        .collect();

    Ok(diff)
}

// async fn main() {
//     let connect_addr =
//         env::args().nth(1).unwrap_or_else(|| panic!("this program requires at least one argument"));
//
//     let url = url::Url::parse(&connect_addr).unwrap();
//
//     let (stdin_tx, stdin_rx) = futures_channel::mpsc::unbounded();
//     tokio::spawn(read_stdin(stdin_tx));
//
//     let (ws_stream, _) = connect_async(url).await.expect("Failed to connect");
//     println!("WebSocket handshake has been successfully completed");
//
//     let (write, read) = ws_stream.split();
//
//     let stdin_to_ws = stdin_rx.map(Ok).forward(write);
//     let ws_to_stdout = {
//         read.for_each(|message| async {
//             let data = message.unwrap().into_data();
//             tokio::io::stdout().write_all(&data).await.unwrap();
//         })
//     };
//
//     pin_mut!(stdin_to_ws, ws_to_stdout);
//     future::select(stdin_to_ws, ws_to_stdout).await;
// }

// Our helper method which will read data from stdin and send it along the
// sender provided.
// async fn read_stdin(tx: futures_channel::mpsc::UnboundedSender<Message>) {
//     let mut stdin = tokio::io::stdin();
//     loop {
//         let mut buf = vec![0; 1024];
//         let n = match stdin.read(&mut buf).await {
//             Err(_) | Ok(0) => break,
//             Ok(n) => n,
//         };
//         buf.truncate(n);
//         tx.unbounded_send(Message::binary(buf)).unwrap();
//     }
// }
