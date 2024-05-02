use std::path::PathBuf;
use eyre::Result;
use reqwest;

use crate::CONFIG;

async fn get_remote_files() -> Result<Vec<PathBuf>> {
    let url = &CONFIG.paths_url();

    let res = reqwest::get(url).await?
        .json::<Vec<String>>()
        .await?
        .iter()
        .map(|s| PathBuf::from(s))
        .collect();

    Ok(res)
}

pub async fn missing_paths(root_path: &PathBuf) -> Result<(Vec<PathBuf>, Vec<PathBuf>)> {
    let local_files = lib::paths::get_files_relative(&root_path)?;
    let remote_files = get_remote_files().await?;

    let local_missing = lib::paths::missing(&local_files, &remote_files);
    let remote_missing = lib::paths::missing(&remote_files, &local_files);

    Ok((local_missing, remote_missing))
}
