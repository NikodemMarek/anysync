use std::path::PathBuf;
use eyre::Result;
use lib::paths::Pth;
use reqwest;

use crate::CONFIG;

async fn get_remote_files() -> Result<Vec<Pth>> {
    let url = &CONFIG.paths_url();

    let res = reqwest::get(url).await?
        .json::<Vec<Pth>>()
        .await?;

    Ok(res)
}

pub async fn missing_paths(root_path: &PathBuf) -> Result<(Vec<Pth>, Vec<Pth>)> {
    let local_files = lib::paths::get_files_relative(&root_path)?;
    let remote_files = get_remote_files().await?;

    let local_missing = lib::paths::missing(&local_files, &remote_files);
    let remote_missing = lib::paths::missing(&remote_files, &local_files);

    Ok((local_missing, remote_missing))
}
