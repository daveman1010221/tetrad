{
  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs";
  };

  outputs = { self, nixpkgs, ... }: let
    # Systems to support
    supportedSystems = [ "x86_64-linux" "aarch64-linux" ];

    # Helper to generate attributes for each system
    forAllSystems = f: nixpkgs.lib.genAttrs supportedSystems (system: f system);

    # The actual Tetrad package definition, parameterized by pkgs and source
    mkTetrad = { pkgs, src }:
      let
        buildMavenPackage = pkgs.callPackage "${nixpkgs}/pkgs/by-name/ma/maven/build-maven-package.nix" {};

        customFetchedMavenDeps = pkgs.stdenv.mkDerivation {
          name = "tetrad-maven-deps";
          inherit src;
          outputHashMode = "recursive";
          outputHashAlgo = "sha256";
          # NOTE: This hash was generated on x86_64-linux.
          # If building on aarch64-linux fails with a hash mismatch,
          # replace it with the correct hash for aarch64.
          outputHash = "sha256-foa+oshSYPOZmNiT3xR6HABgJDVCUJIU0qiin2GDPJI=";

          nativeBuildInputs = [ pkgs.xmlstarlet pkgs.xmllint pkgs.maven pkgs.jdk25 ];

          patchPhase = ''
            echo "Patching pom.xml to disable Javadoc plugin and Sonatype Central publishing plugin"
            xmlstarlet ed -L \
              -d "/project/build/plugins/plugin[groupId='org.apache.maven.plugins' and artifactId='maven-javadoc-plugin']" \
              -d "/project/build/plugins/plugin[groupId='org.sonatype.central' and artifactId='central-publishing-maven-plugin']" \
              pom.xml
            xmllint --noout pom.xml
          '';

          buildPhase = ''
            echo "Fetching Maven dependencies offline"
            export JAVA_HOME=${pkgs.jdk25}
            export PATH=$JAVA_HOME/bin:$PATH
            mvn dependency:resolve -Dmaven.repo.local=$out/.m2 -DskipTests -Dmaven.javadoc.skip=true -Dgpg.skip=true
          '';

          installPhase = ''
            echo "Dependencies fetched to $out/.m2"
          '';
        };
      in
        buildMavenPackage {
          pname = "tetrad";
          version = src.shortRev or "dirty";
          inherit src;
          fetchedMavenDeps = customFetchedMavenDeps;
          mvnParameters = "-DskipTests -Dmaven.javadoc.skip=true -Dgpg.skip=true";
          doCheck = false;
          installPhase = ''
            mkdir -p $out/share/java
            cp tetrad-gui/target/tetrad-gui-*-launch.jar $out/share/java/
          '';
          # Same hash as the customFetchedMavenDeps derivation above
          mvnHash = "sha256-foa+oshSYPOZmNiT3xR6HABgJDVCUJIU0qiin2GDPJI=";
        };
  in {
    packages = forAllSystems (system: let
      pkgs = import nixpkgs { inherit system; };
    in {
      default = mkTetrad { pkgs = pkgs; src = self; };
    });
  };
}
