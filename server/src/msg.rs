pub enum Msg {
    Text(String),
    Binary(Vec<u8>),
    Ping(Vec<u8>),
    Pong(Vec<u8>),
    Close(Option<tokio_tungstenite::tungstenite::protocol::CloseFrame<'static>>),
    Frame(tokio_tungstenite::tungstenite::protocol::frame::Frame),
}
impl From<tokio_tungstenite::tungstenite::Message> for Msg {
    fn from(msg: tokio_tungstenite::tungstenite::Message) -> Self {
        use tokio_tungstenite::tungstenite::Message;

        match msg {
            Message::Text(txt) => Self::Text(txt),
            Message::Binary(bin) => Self::Binary(bin),
            Message::Ping(ping) => Self::Ping(ping),
            Message::Pong(pong) => Self::Pong(pong),
            Message::Close(close) => Self::Close(close),
            Message::Frame(frame) => Self::Frame(frame),
        }
    }
}
impl From<axum::extract::ws::Message> for Msg {
    fn from(msg: axum::extract::ws::Message) -> Self {
        use axum::extract::ws::{Message, close_code};
        use tokio_tungstenite::tungstenite::protocol::frame::coding::CloseCode;

        match msg {
            Message::Text(txt) => Self::Text(txt),
            Message::Binary(bin) => Self::Binary(bin),
            Message::Ping(ping) => Self::Ping(ping),
            Message::Pong(pong) => Self::Pong(pong),
            Message::Close(close) => Self::Close(close.map(|cf| tokio_tungstenite::tungstenite::protocol::CloseFrame {
                code: match cf.code {
                    close_code::AWAY => CloseCode::Away,
                    close_code::UNSUPPORTED => CloseCode::Unsupported,
                    close_code::PROTOCOL => CloseCode::Protocol,
                    close_code::NORMAL => CloseCode::Normal,
                    close_code::SIZE => CloseCode::Size,
                    close_code::INVALID => CloseCode::Invalid,
                    close_code::AGAIN => CloseCode::Again,
                    close_code::ERROR => CloseCode::Error,
                    close_code::POLICY => CloseCode::Policy,
                    close_code::STATUS => CloseCode::Status,
                    close_code::RESTART => CloseCode::Restart,
                    close_code::ABNORMAL => CloseCode::Abnormal,
                    close_code::EXTENSION => CloseCode::Extension,
                    _ => CloseCode::Error
                },
                reason: std::borrow::Cow::Owned(cf.reason.into_owned()),
            })),
        }
    }
}
impl From<Msg> for axum::extract::ws::Message {
    fn from(msg: Msg) -> Self {
        use axum::extract::ws::{close_code, CloseFrame};
        use tokio_tungstenite::tungstenite::protocol::frame::coding::CloseCode;

        match msg {
            Msg::Text(txt) => Self::Text(txt),
            Msg::Binary(bin) => Self::Binary(bin),
            Msg::Ping(ping) => Self::Ping(ping),
            Msg::Pong(pong) => Self::Pong(pong),
            Msg::Close(close) => Self::Close(close.map(|cf| CloseFrame {
                code: match cf.code {
                    CloseCode::Away => close_code::AWAY,
                    CloseCode::Unsupported => close_code::UNSUPPORTED,
                    CloseCode::Protocol => close_code::PROTOCOL,
                    CloseCode::Normal => close_code::NORMAL,
                    CloseCode::Size => close_code::SIZE,
                    CloseCode::Invalid => close_code::INVALID,
                    CloseCode::Again => close_code::AGAIN,
                    CloseCode::Error => close_code::ERROR,
                    CloseCode::Policy => close_code::POLICY,
                    CloseCode::Status => close_code::STATUS,
                    CloseCode::Restart => close_code::RESTART,
                    CloseCode::Abnormal => close_code::ABNORMAL,
                    CloseCode::Extension => close_code::EXTENSION,
                    _ => close_code::ERROR
                },
                reason: std::borrow::Cow::Owned(cf.reason.into_owned()),
            })),
            Msg::Frame(frame) => axum::extract::ws::Message::Binary(frame.payload().to_vec()),
        }
    }
}

