{
  inputs = {
    cargo2nix.url = "github:cargo2nix/cargo2nix/release-0.11.0";
    flake-utils.follows = "cargo2nix/flake-utils";
    nixpkgs.follows = "cargo2nix/nixpkgs";
  };

  outputs = inputs:
    with inputs;
      flake-utils.lib.eachDefaultSystem (
        system: let
          pkgs = import nixpkgs {
            inherit system;
            overlays = [cargo2nix.overlays.default];
          };

          rustPkgs = pkgs.rustBuilder.makePackageSet {
            rustVersion = "1.75.0";
            packageFun = import ./Cargo.nix;
          };

          workspaceShell = let
            alias-run = pkgs.writeShellScriptBin "r" ''cd server; cargo run'';
            alias-dev = pkgs.writeShellScriptBin "d" ''cargo-watch -C server -x 'run -- -p 5070' -c'';
            alias-test = pkgs.writeShellScriptBin "t" ''cargo-watch -C server -x test -c'';

            alias-get-material-icon =
              pkgs.writeShellScriptBin
              "get-material-icon"
              ''curl -o ./app/app/src/main/res/drawable/$1_rounded_48.xml "https://fonts.gstatic.com/s/i/short-term/release/materialsymbolsrounded/$1/default/48px.xml"'';
          in
            rustPkgs.workspaceShell
            {
              packages = [cargo2nix.packages."${system}".cargo2nix pkgs.cargo-watch];
              buildInputs = [alias-run alias-dev alias-test alias-get-material-icon];
              shellHook = ''
                printf "\e[33m
                  \e[1mr\e[0m\e[33m  -> run
                  \e[1md\e[0m\e[33m  -> dev
                  \e[1mt\e[0m\e[33m  -> tests

                  \e[1mget-material-icon\e[0m\e[33m -> fetch material icon
                \e[0m"
              '';
            };
        in rec {
          devShells = {
            default = workspaceShell;
          };

          packages = {
            anysync-server = rustPkgs.workspace.anysync-server {};
            default = packages.anysync-server;
          };
        }
      );
}
