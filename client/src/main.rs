use std::path::PathBuf;
use clap::Parser;
use futures_util::future::join_all;

mod ws_transfer_handlers;
mod check;

#[derive(Parser, Debug)]
#[command(version, about, long_about = None)]
struct Args {
    #[arg(short, long, default_value = "../testdircopy")]
    root_path: PathBuf,

    #[arg(short, long, default_value = "5060")]
    port: u16,
}

#[tokio::main]
async fn main() {
    let Args { root_path, port } = Args::parse();

    let (local_paths, remote_paths) = check::missing_paths(&root_path).await
        .expect("failed to get missing local files");

    // Sync remote paths to local
    let tasks = local_paths
        .iter()
        .map(|p| ws_transfer_handlers::get_file(&root_path, p));

    join_all(tasks).await
        .iter()
        .for_each(|r| {
            if let Err(e) = r {
                eprintln!("error while receiving file: {}", e);
            }
        });

    // Sync local paths to remote
    let tasks = remote_paths
        .iter()
        .map(|p| ws_transfer_handlers::set_file(&root_path , p));

    join_all(tasks).await
        .iter()
        .for_each(|r| {
            if let Err(e) = r {
                eprintln!("error while sending file: {}", e);
            }
        });
}
