use std::path::PathBuf;
use clap::Parser;
use eyre::OptionExt;

#[derive(Debug, serde::Deserialize, Clone)]
pub struct Config {
    pub port: u16,
    pub sources: std::collections::HashMap<String, SourceConfig>,
}

#[derive(Debug, serde::Deserialize, Clone)]
pub struct SourceConfig {
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
    let cli_args = CliArgs::parse();

    let config_path = if let Some(config_path) = cli_args.config.clone() {
        Some(config_path)
    } else if let Some(config_dir) = dirs::config_dir() {
        Some(config_dir.join("anysync/config.toml"))
    } else {
        None
    }.ok_or_eyre("failed to get config file location")?;

    let config_dir = config_path.parent()
        .ok_or_eyre("failed to get parent directory of config file")?;

    if !std::path::Path::new(&config_path).exists() {
        std::fs::create_dir_all(config_dir)?;
        std::fs::write(&config_path, format!("port = {}\n\n[sources]", cli_args.port.unwrap_or(5060)))?;
    }

    let conf = config::Config::builder()
        .add_source(DefaultConfig)
        .add_source(config::File::with_name(&config_path.to_string_lossy().to_string()))
        .add_source(cli_args)
        .build()?.try_deserialize::<Config>()?;


    let sources = conf.sources.iter().map(|(k, v)| {
        (k.clone(), SourceConfig {
            path: if v.path.is_absolute() {
                v.path.clone()
            } else {
                config_dir.join(&v.path)
            },
            actions: v.actions.clone(),
        })
    }).collect();

    Ok(Config {
        port: conf.port,
        sources,
    })
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
    config: Option<PathBuf>,

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
