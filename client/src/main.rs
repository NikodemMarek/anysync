use futures_util::future::join_all;
use lazy_static::lazy_static;

mod ws_transfer_handlers;
mod check;
mod config;

lazy_static! {
    static ref CONFIG: config::Config = config::get_final()
        .expect("failed to get config");
}

#[tokio::main]
async fn main() {
    let (local_paths, remote_paths) = check::missing_paths(&CONFIG.dir).await
        .expect("failed to get missing local files");

    // Sync remote paths to local
    let tasks = local_paths
        .iter()
        .map(|p| ws_transfer_handlers::get_file(&CONFIG.dir, p));

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
        .map(|p| ws_transfer_handlers::set_file(&CONFIG.dir , p));

    join_all(tasks).await
        .iter()
        .for_each(|r| {
            if let Err(e) = r {
                eprintln!("error while sending file: {}", e);
            }
        });
}
