{pkgs ? import <nixpkgs> {}}: let
  alias-run = pkgs.writeShellScriptBin "r" ''cargo -Z unstable-options -C server run'';
  alias-dev = pkgs.writeShellScriptBin "d" ''cargo-watch -C server -x run -c'';
  alias-test = pkgs.writeShellScriptBin "t" ''cargo-watch -C server -x test -c'';
  alias-browser = pkgs.writeShellScriptBin "b" ''qutebrowser -r anysync &'';

  alias-logcat =
    pkgs.writeShellScriptBin
    "logcat"
    ''adb shell run-as com.example.anysync logcat'';
  alias-get-material-icon =
    pkgs.writeShellScriptBin
    "get-material-icon"
    ''curl -o ./app/app/src/main/res/drawable/$1_rounded_48.xml "https://fonts.gstatic.com/s/i/short-term/release/materialsymbolsrounded/$1/default/48px.xml"'';
in
  pkgs.mkShell {
    buildInputs = with pkgs;
      [
        gcc
        rustup
        rustfmt
        cargo
        cargo-watch
        openssl
        pkg-config
        android-tools
      ]
      ++ [alias-run alias-dev alias-test alias-browser alias-logcat alias-get-material-icon];
    shellHook = ''
      rustup default nightly
      adb -a -P 5037 server

      printf "\e[33m
        \e[1mr\e[0m\e[33m  -> run
        \e[1md\e[0m\e[33m  -> dev
        \e[1mt\e[0m\e[33m -> tests

        \e[1mlogcat\e[0m\e[33m -> run logcat
        \e[1mget-material-icon\e[0m\e[33m -> fetch material icon

        \e[1mb\e[0m\e[33m -> restore browser session
      \e[0m"
    '';
  }
