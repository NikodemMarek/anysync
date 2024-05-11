use std::path::PathBuf;
use clap::Parser;

#[derive(Debug, serde::Deserialize, Clone)]
pub struct Config {
    pub port: u16,
    pub sources: std::collections::HashMap<String, SourceConfig>,
}

#[derive(Debug, serde::Deserialize, Clone)]
pub struct SourceConfig {
    pub name: String,
    pub dir: PathBuf,
}

pub fn get_final() -> eyre::Result<Config> {
    Ok(config::Config::builder()
        .add_source(DefaultConfig)
        .add_source(config::File::with_name("../config.toml"))
        .add_source(CliArgs::parse())
        .build()?
        .try_deserialize()?)
}

#[derive(Debug, Clone)]
struct DefaultConfig;
impl config::Source for DefaultConfig {
    fn clone_into_box(&self) -> Box<dyn config::Source + Send + Sync> {
        Box::new(self.clone())
    }
    fn collect(&self) -> Result<config::Map<String, config::Value>, config::ConfigError> {
        use config::Value;
        let mut map = std::collections::HashMap::new();
        map.insert(
            "port".to_string(),
            Value::new(
                Some(&String::from("port")),
                5060
            )
        );
        Ok(map)
    }
}

#[derive(clap::Parser, Debug, Clone)]
#[command(version, about, long_about = None)]
struct CliArgs {
    #[arg(short, long)]
    port: Option<u16>,
}
impl config::Source for CliArgs {
    fn clone_into_box(&self) -> Box<dyn config::Source + Send + Sync> {
        Box::new(self.clone())
    }

    fn collect(&self) -> Result<config::Map<String, config::Value>, config::ConfigError> {
        use config::Value;

        let mut map = std::collections::HashMap::new();
        if let Some(port) = &self.port {
            map.insert(
                "port".to_string(),
                Value::new(
                    Some(&String::from("port")),
                    *port
                )
            );
        }
        Ok(map)
    }
}
