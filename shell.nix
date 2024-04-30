{pkgs ? import <nixpkgs> {}}: let
  alias-run = pkgs.writeShellScriptBin "r" ''cargo -Z unstable-options -C server run'';
  alias-dev = pkgs.writeShellScriptBin "d" ''cargo-watch -C server -x run -c'';
  alias-test = pkgs.writeShellScriptBin "t" ''cargo-watch -C server -x test -c'';
  alias-browser = pkgs.writeShellScriptBin "b" ''qutebrowser -r anysync &'';
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
      ]
      ++ [alias-run alias-dev alias-test alias-browser];
    shellHook = ''
      rustup default nightly
      printf "\e[33m
        \e[1mr\e[0m\e[33m  -> run
        \e[1md\e[0m\e[33m  -> dev
        \e[1mt\e[0m\e[33m -> tests

        \e[1mb\e[0m\e[33m -> restore browser session
      \e[0m"
    '';
  }
