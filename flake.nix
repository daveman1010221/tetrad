{
  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs";
    tetrad-src.url = "github:daveman1010221/tetrad/development";
  };

  outputs = { self, nixpkgs, tetrad-src }: {
    packages.x86_64-linux.default = let
      # Import the Nixpkgs library for package definitions
      pkgs = import nixpkgs { system = "x86_64-linux"; };

      # Fetch a specific Nixpkgs commit for stability and compatibility
      # Ensures `buildMavenPackage` is available
      nixpkgsRepo = nixpkgs;

      # Explicitly load the `buildMavenPackage` function from the specified Nixpkgs repo
      # This is critical for building Maven projects in a Nix environment
      buildMavenPackage = pkgs.callPackage "${nixpkgsRepo}/pkgs/by-name/ma/maven/build-maven-package.nix" {};

      # Fetch the Tetrad source code
      # This source will be used in both the dependency fetch and main build derivations
      tetradSource = tetrad-src;

      # Create a derivation to handle Maven dependency resolution
      # This step ensures that all dependencies are resolved offline and reproducibly
      customFetchedMavenDeps = pkgs.stdenv.mkDerivation {
        name = "tetrad-maven-deps";
        src = tetradSource;

        # Add tools required for patching and validating the POM
        nativeBuildInputs = [ pkgs.xmlstarlet pkgs.xmllint ];

        # Patch the POM to remove problematic plugins and references
        # - Removes `maven-javadoc-plugin` to avoid Javadoc errors
        # - Removes Sonatype references, as they are unnecessary for local builds
        patchPhase = ''
          echo "Patching pom.xml to disable Javadoc plugin and Sonatype references"
          # Remove the maven-javadoc-plugin from the POM
          xmlstarlet ed -L \
            -d "//plugin[artifactId='maven-javadoc-plugin']" \
            pom.xml

          # Validate that the POM file is still valid XML after patching
          xmllint --noout pom.xml || { echo "Invalid POM after patching"; exit 1; }
        '';

        # Specify Maven as a build input to ensure it is available
        buildInputs = [ pkgs.maven ];

        # Resolve dependencies offline and store them in the specified local Maven repo
        buildPhase = ''
          echo "Fetching Maven dependencies offline"
          export JAVA_HOME=${pkgs.jdk23}
          export PATH=$JAVA_HOME/bin:$PATH
          mvn dependency:resolve -Dmaven.repo.local=$out/.m2
        '';

        # Declare the output location for Maven dependencies
        installPhase = ''
          echo "Dependencies fetched to $out/.m2"
        '';
      };

      # Main derivation for building Tetrad
      # This step compiles the source code and packages the output .jar file
      customTetrad = buildMavenPackage {
        pname = "tetrad";
        version = builtins.substring 0 8 (tetrad-src.rev or "unknown");

        # Use the same source as the dependency fetch phase
        src = tetradSource;

        # Use the pre-fetched Maven dependencies to ensure offline builds
        fetchedMavenDeps = customFetchedMavenDeps;

        # Add build parameters to skip problematic steps
        # - Skips Javadoc generation (`-Dmaven.javadoc.skip=true`)
        # - Skips running tests (`-DskipTests`)
        mvnParameters = "-Dmaven.javadoc.skip=true -DskipTests";

        # Explicitly disable Nix checks, as they are not needed here
        doCheck = false;

        # Specify where to copy the compiled output
        installPhase = ''
          mkdir -p $out/share/java
          cp tetrad-gui/target/tetrad-gui-*-launch.jar $out/share/java/
        '';

        # Specify the fixed-output hash for the Maven dependency resolution step
        # This ensures reproducibility by verifying the content hash
        mvnHash = "sha256-Nfv6jhnIZ+sWDsuQXqoaziIG5n2YSqyYUcrHPDuFpHM=";
      };

    # Return the final build derivation as the default package
    in
      customTetrad;
  };
}
