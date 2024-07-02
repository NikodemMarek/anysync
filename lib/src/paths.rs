use eyre::{Ok, Result};
use serde::{Deserialize, Serialize};
use std::{
    fs::{self, DirEntry},
    path::PathBuf, time::SystemTime,
};

#[derive(Debug, Clone, Ord, PartialOrd, Eq, PartialEq, Deserialize, Serialize)]
pub struct Pth {
    pub path: PathBuf,
    pub modified: Option<u128>,
}

impl From<DirEntry> for Pth {
    fn from(entry: DirEntry) -> Self {
        Pth {
            path: entry.path(),
            modified: entry.metadata().ok().and_then(|m| m.modified().ok()).map(|v| v.duration_since(SystemTime::UNIX_EPOCH).ok().map(|v| v.as_millis())).unwrap_or(None),
        }
    }
}

pub fn get_files(path: &PathBuf) -> Result<Vec<Pth>> {
    let files = fs::read_dir(path)?.try_fold(Vec::new(), |mut acc, entry| {
        let path = Pth::from(entry?);

        if path.path.is_dir() {
            acc.extend(get_files(&path.path)?);
            return Ok(acc);
        }

        acc.push(path);
        Ok(acc)
    })?;

    Ok(files)
}

pub fn get_files_relative(path: &PathBuf) -> Result<Vec<Pth>> {
    let files = get_files(path)?
        .iter()
        .map(|p| Pth { path: p.path.strip_prefix(&path).unwrap().to_path_buf(), modified: p.modified })
        .collect();

    Ok(files)
}

pub fn missing<'a, T: Ord + Clone>(has: &'a [T], wants: &'a [T]) -> Vec<T> {
    wants
        .iter()
        .filter(|a| !has.iter().any(|b| *a == b))
        .map(|p| p.clone())
        .collect()
}
