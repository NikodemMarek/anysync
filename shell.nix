{pkgs ? import <nixpkgs> {}}: let
  alias-run = pkgs.writeShellScriptBin "r" ''cargo -Z unstable-options -C server run'';
  alias-dev = pkgs.writeShellScriptBin "d" ''cargo-watch -C server -x run -c'';
  alias-test = pkgs.writeShellScriptBin "t" ''cargo-watch -C server -x test -c'';
in
  pkgs.mkShell {
    buildInputs = with pkgs;
      [
        gcc
        rustup
        rustfmt
        cargo
        cargo-watch
      ]
      ++ [alias-run alias-dev alias-test];
    shellHook = ''
      rustup default nightly
      printf "\e[33m
        \e[1mr\e[0m\e[33m  -> run
        \e[1md\e[0m\e[33m  -> dev
        \e[1mt\e[0m\e[33m -> tests
      \e[0m"
    '';
  }
