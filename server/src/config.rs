use std::path::PathBuf;
use clap::Parser;

const INITIAL_CONFIG_FILE: &str = r#"
port = 5060

[sources]
"#;

#[derive(Debug, serde::Deserialize, Clone)]
pub struct Config {
    pub port: u16,
    pub sources: std::collections::HashMap<String, SourceConfig>,
}

#[derive(Debug, serde::Deserialize, Clone)]
pub struct SourceConfig {
    pub name: String,
    pub path: PathBuf,
    pub actions: Actions,
}

#[derive(Debug, serde::Deserialize, Clone)]
pub enum Actions {
    None,
    Get,
    Set,
    GetSet,
}

pub fn get_final() -> eyre::Result<Config> {
    let mut builder = config::Config::builder();

    builder = builder.add_source(DefaultConfig);
    if let Some(config_dir) = dirs::config_dir() {
        let path = config_dir.join("anysync/config.toml").to_string_lossy().to_string();
        if !std::path::Path::new(&path).exists() {
            std::fs::create_dir_all(config_dir.join("anysync"))?;
            std::fs::write(&path, INITIAL_CONFIG_FILE)?;
        }

        builder = builder.add_source(config::File::with_name(&path));
    }
    builder = builder.add_source(CliArgs::parse());

    Ok(builder.build()?.try_deserialize()?)
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
