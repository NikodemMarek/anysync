use serde::{Deserialize, Serialize, ser::SerializeStruct};

use crate::paths::Pth;

#[derive(Debug, Clone, Deserialize)]
pub struct Diff(Vec<String>, Vec<String>, Vec<String>, Vec<String>, Vec<String>, Vec<String>); // (modified, removed, new) (server, client)
impl Serialize for Diff {
    fn serialize<S>(&self, serializer: S) -> Result<S::Ok, S::Error>
    where
        S: serde::Serializer,
    {
        let Diff(modified_server, removed_server, new_server, modified_client, removed_client, new_client) = self;
        let mut state = serializer.serialize_struct("Diff", 3)?;
        state.serialize_field("modified_server", modified_server)?;
        state.serialize_field("removed_server", removed_server)?;
        state.serialize_field("new_server", new_server)?;
        state.serialize_field("modified_client", modified_client)?;
        state.serialize_field("removed_client", removed_client)?;
        state.serialize_field("new_client", new_client)?;
        state.end()
    }
}

pub fn diff<'a>(server: &'a [Pth], client: &'a [Pth], last_sync: Option<u128>) -> Diff {
    let mut modified_server = Vec::new();
    let mut removed_server = Vec::new();
    let mut new_server = Vec::new();

    let mut modified_client = Vec::new();
    let mut removed_client = Vec::new();
    let mut new_client = Vec::new();

    for s in server {
        if let Some(c) = client.iter().find(|c| c.path == s.path) {
            if s.modified > c.modified {
                modified_server.push(s.path.to_string_lossy().to_string());
            } else if s.modified < c.modified {
                modified_client.push(c.path.to_string_lossy().to_string());
            }
        } else {
            if let Some(last_sync) = last_sync {
                if let Some(m) = s.modified {
                    if m > last_sync {
                        new_server.push(s.path.to_string_lossy().to_string());
                    } else {
                        removed_client.push(s.path.to_string_lossy().to_string());
                    }
                } else {
                    new_server.push(s.path.to_string_lossy().to_string());
                }
            } else {
                new_server.push(s.path.to_string_lossy().to_string());
            }
        }
    }
    for c in client {
        if !server.iter().any(|s| s.path == c.path) {
            if let Some(last_sync) = last_sync {
                if let Some(m) = c.modified {
                    if m > last_sync {
                        new_client.push(c.path.to_string_lossy().to_string());
                    } else {
                        removed_server.push(c.path.to_string_lossy().to_string());
                    }
                } else {
                    new_client.push(c.path.to_string_lossy().to_string());
                }
            } else {
                new_client.push(c.path.to_string_lossy().to_string());
            }
        }
    }

    Diff(modified_server, removed_server, new_server, modified_client, removed_client, new_client)
}
