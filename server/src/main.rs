use eyre::Result;
use std::fs;
use std::path::PathBuf;

fn get_files(path: &PathBuf) -> Result<Vec<PathBuf>> {
    let paths = fs::read_dir(path)?;

    let mut files = Vec::new();
    for path in paths {
        let path = path?.path();

        if path.is_dir() {
            if let Ok(items) = get_files(&path) {
                files.extend(items);
            } else {
                eprintln!("Error reading directory: {:?}", path.display());
            }
        } else if path.is_file() {
            files.push(path);
        }
    }

    Ok(files)
}

fn main() {
    let root_path = PathBuf::from("../testdir");
    let files = get_files(&root_path).unwrap();
    dbg!(files);
}
