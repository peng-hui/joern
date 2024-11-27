import os, subprocess
import argparse, re
SCAN_EXT=".scan"
UPDATE_EXT=".update"


def analyze(lib, libdir, outdir, scanner):
    file_ext = SCAN_EXT if scanner == "scan" else UPDATE_EXT
    
    command1 = f"./joern-scan {libdir} > {outdir}/{lib}{file_ext}"
    command2 = f"./joern --script ./update.sc --param inputPath={libdir} --param outputPath={outdir}/{lib}{file_ext}"
    try:
        command = command1 if scanner == "scan" else command2
        #print(command)
        #print("%s:%s"%(lib, libdir))
        subprocess.run(command, shell=True, check=True, stdout=subprocess.DEVNULL)
    except subprocess.CalledProcessError as e:
        print("ERROR %s: %d" % (lib, e.returncode))

def main():
    """Main function. Collect command line arguments and begin"""
    parser = argparse.ArgumentParser()

    parser.add_argument(
        "--scanner",
        default="update",
        help=(
            "Specify the scanner:"
            "--scanner scan/update"
        ),
    )

    parser.add_argument(
        "--outdir",
        default="results",
        help=(
            "Output parent directory where the analysis results are saved. "
            "If not set, it would be currentdir/results."
        ),
    )

    parser.add_argument(
        "--library",
        help=(
            "One library for analysis "
            "'libname:path/to/lib'"
        ),
    )

    parser.add_argument(
        "--batch",
        help=(
            "Dataset file to specify batch analysis"
        ),
    )

    args = parser.parse_args()

    assert args.scanner == "scan" or args.scanner == "update", "--scanner must be scan/update"


    if not os.path.exists(args.outdir):
        os.mkdir(args.outdir)   

    if args.library:
        lib, libdir = args.library.strip().split(":")

        analyze(lib, libdir, args.outdir, args.scanner)

    elif args.batch and os.path.exists(args.batch):
        with open(args.batch, "r") as fp:
            libs = [i.strip().split(":") for i in fp.readlines()]
        for lib in libs:
            analyze(lib[0], lib[1], args.outdir, args.scanner)
    else:
        print("need to either specify a library (--library) or a dataset file in batch (--batch) ")

if __name__ == "__main__":
    main()