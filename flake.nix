{
  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
    flake-utils.url = "github:numtide/flake-utils";
  };

  outputs = { self, nixpkgs, flake-utils }:
    flake-utils.lib.eachDefaultSystem (system:
      let
        pkgs = nixpkgs.legacyPackages.${system};
        in rec {
        devShell = with pkgs;
        mkShell {
          JDK_8 = "${jdk8}";
          buildInputs = [
            jdk8
          ];
        };
      }
   );
}
