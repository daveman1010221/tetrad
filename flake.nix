{
  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs";
  };

  outputs = { self, nixpkgs, ... }: let
    supportedSystems = [ "x86_64-linux" "aarch64-linux" ];
    forAllSystems = f: nixpkgs.lib.genAttrs supportedSystems (system: f system);

    mkTetrad = { pkgs, src }:
      let
        buildMavenPackage = pkgs.callPackage "${nixpkgs}/pkgs/by-name/ma/maven/build-maven-package.nix" {};
      in
        buildMavenPackage {
          pname = "tetrad";
          version = src.shortRev or "dirty";
          inherit src;
          mvnJdk = pkgs.jdk21;
          nativeBuildInputs = [ pkgs.xmlstarlet pkgs.libxml2 ];
          prePatch = ''
            echo "Patching pom.xml to disable Javadoc and Sonatype Central publishing plugins"
            xmlstarlet ed -L \
              -d "/project/build/plugins/plugin[groupId='org.apache.maven.plugins' and artifactId='maven-javadoc-plugin']" \
              -d "/project/build/plugins/plugin[groupId='org.sonatype.central' and artifactId='central-publishing-maven-plugin']" \
              pom.xml
            xmllint --noout pom.xml
          '';
          mvnParameters = "-DskipTests -Dmaven.javadoc.skip=true -Dgpg.skip=true";
          doCheck = false;
          installPhase = ''
            mkdir -p $out/share/java
            cp tetrad-gui/target/tetrad-gui-*-launch.jar $out/share/java/
          '';
          mvnHash = "sha256-3fdBkGUHbUEzcAsqkuur7ptD6kb/OsKNyj20BivMmI4=";
        };

  in {
    packages = forAllSystems (system: let
      pkgs = import nixpkgs { inherit system; };
    in {
      default = mkTetrad { pkgs = pkgs; src = self; };
    });

    devShells = forAllSystems (system: let
      pkgs = import nixpkgs { inherit system; };
    in {
      default = pkgs.mkShell {
        name = "tetrad-dev";
        packages = [
          pkgs.jdk21
          pkgs.maven
          pkgs.xmlstarlet
          pkgs.libxml2
          pkgs.git
        ];
        shellHook = ''
          export JAVA_HOME=${pkgs.jdk21}
          export PATH=$JAVA_HOME/bin:$PATH
          echo "Tetrad dev shell — Java $(java -version 2>&1 | head -1)"
        '';
      };
    });
  };
}
