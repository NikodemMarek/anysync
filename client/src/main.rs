use std::path::PathBuf;
use clap::Parser;

mod get_file;

#[derive(Parser, Debug)]
#[command(version, about, long_about = None)]
struct Args {
    #[arg(short, long, default_value = "../testdir")]
    root_path: PathBuf,

    #[arg(short, long, default_value = "5060")]
    port: u16,
}

#[tokio::main]
async fn main() {
    let Args { root_path, port } = Args::parse();

    let fl = get_file::get_missing_local_files(&root_path).await;
    println!("{:?}", fl);
    let r = get_file::get_file(&root_path, "test1.txt").await;
    println!("{:?}", r);
    let r = get_file::set_file(&root_path, "testoooo").await;
    println!("{:?}", r);
}
