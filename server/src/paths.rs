use eyre::{Ok, Result};
use std::{fs, path::PathBuf};

pub fn get_files(path: &PathBuf) -> Result<Vec<PathBuf>> {
    let files = fs::read_dir(path)?
        .try_fold(Vec::new(), |mut acc, entry| {
            let path = entry?.path();

            if path.is_dir() {
                acc.extend(get_files(&path)?);
                return Ok(acc);
            }

            acc.push(path);
            Ok(acc)
        })?;

    Ok(files)
}

pub fn get_files_relative(path: &PathBuf) -> Result<Vec<PathBuf>> {
    let files = get_files(path)?
        .iter()
        .map(|p| p.strip_prefix(&path).unwrap().to_path_buf())
        .collect();

    Ok(files)
}

pub fn missing<'a, T: Ord + Clone>(has: &'a [T], wants: &'a [T]) -> Vec<T> {
    wants.iter()
        .filter(|a| !has.iter().any(|b| *a == b))
        .map(|p| p.clone())
        .collect()
}
