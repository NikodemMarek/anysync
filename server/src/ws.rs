use axum::{
    extract::{ws::{Message, WebSocket, WebSocketUpgrade}, Path},
    response::IntoResponse,
};
use axum_extra::TypedHeader;
use std::{borrow::Cow, io::Write, net::SocketAddr, path::PathBuf};
use axum::extract::{connect_info::ConnectInfo, ws::CloseFrame};
use futures::{sink::SinkExt, stream::{SplitSink, StreamExt}};
use eyre::Result;
use std::io::Read;

pub async fn ws_handler(
    Path(file): Path<String>,

    ws: WebSocketUpgrade,
    user_agent: Option<TypedHeader<headers::UserAgent>>,
    ConnectInfo(addr): ConnectInfo<SocketAddr>,
) -> impl IntoResponse {
    let user_agent = if let Some(TypedHeader(user_agent)) = user_agent {
        user_agent.to_string()
    } else {
        String::from("Unknown browser")
    };
    let file = PathBuf::from(format!("../testdir/{}", file));

    println!("`{user_agent}` at {addr} connected.");
    println!("Requested file: {}", file.display());

    ws.on_upgrade(move |socket| handle_socket(file, socket, addr))
}

async fn handle_socket(file: PathBuf, mut socket: WebSocket, who: SocketAddr) {
    if socket.send(Message::Ping(vec![1, 2, 3])).await.is_ok() {
        println!("Pinged {who}...");
    } else {
        println!("Could not send ping {who}!");
        return;
    }

    if let Some(msg) = socket.recv().await {
        if msg.is_err() {
            println!("client {who} abruptly disconnected");
            return;
        }
    }

    let (mut sender, _) = socket.split();

    let mut send_task: tokio::task::JoinHandle<Result<_>> = tokio::spawn(async move {
        let mut file = std::fs::File::open(file)?;
        let chunk_size = 0x4000;

        sender
            .send(Message::Text("starting file transfer".to_string()))
            .await?;

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
            .send(Message::Text("transfer finished".to_string()))
            .await?;

        println!("Sending close to {who}...");
        if let Err(e) = sender
            .send(Message::Close(Some(CloseFrame {
                code: axum::extract::ws::close_code::NORMAL,
                reason: Cow::from("Goodbye"),
            })))
            .await
        {
            println!("Could not send Close due to {e}, probably it is ok?");
        }

        return Ok(());
    });

    tokio::select! {
        rv_a = (&mut send_task) => {
            match rv_a {
                Ok(a) => {
                    match a {
                        Ok(_) => println!("Send task completed successfully"),
                        Err(e) => println!("Send task failed with {e}")
                    };
                }
                Err(a) => println!("Error sending messages {a:?}")
            }
        },
    }

    // returning from the handler closes the websocket connection
    println!("Websocket context {who} destroyed");
}
