import os, subprocess
import argparse, re


# Regular expression to find the lines with 'Result: 8.0' and associated JS file and line numbers
pattern = r'^Result: 8\.0.*?([\w./-]+:\d+):'
pattern1 = r'^[\w./-]+:\d+$'
BENCHDIR="/Users/phli/llm4pa/joern/benchmark/"
SCAN_EXT=".scan"
EXPECTED_EXT=".expected"
UPDATE_EXT=".update"


AGround = 0
ATP = 0
AFP = 0

FN_LIBs = []

def clean(redir, scanner):
    file_ext = SCAN_EXT if scanner == "scan" else UPDATE_EXT
    
    if not redir.endswith(file_ext):
        return
    global AGround, ATP, AFP, FN_LIBs
    
    result_pattern = pattern if scanner == "scan" else pattern1

    with open(redir, "r") as fp:
        content = fp.read().replace("/", ".")
        results = re.findall(result_pattern, content, re.MULTILINE)


    libname = BENCHDIR + redir.split("/")[-1].removesuffix(file_ext) + "_lib"

    # print(libname)
    TP = 0
    FP = 0
    ground = 0
    content = ""

    for f in os.listdir(libname):
        if f.endswith(EXPECTED_EXT):
            with open(os.path.join(libname, f), "r") as fp:
                tempcontent = fp.readlines()
                ground += len(tempcontent)
            content += "\n" + "".join(tempcontent).replace("/", ".")

    for r in results:
        if r in content:
            TP += 1
        else:
            FP += 1

    with open(redir.removesuffix(file_ext) + ".cleaned", "a+") as fp:
        fp.write("===GROUND-TRUTH===\n")
        fp.write(content)
        fp.write("\n\n")
    
        fp.write(f"==={file_ext}===\n")
        fp.write(f"Ground:{ground}\tTP:{TP}\tFN:{ground-TP}\tFP:{FP}\n")
        fp.write("\n".join(results))
        fp.write("\n\n")


    AGround += ground
    ATP += TP
    AFP += FP

    if ground - TP > 0:
        FN_LIBs.append(redir)

def main():
    """Main function. Collect command line arguments and begin"""
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--outdir",
        help=(
            "Output parent directory where the analysis results are saved. "
            "If not set, it would be currentdir/results."
        ),
    )

    parser.add_argument(
        "--scanner",
        default="update",
        help=(
            "Spficify the scanner that produced the results"
            "--scanner scan/update"
        ),
    )

    args = parser.parse_args()
    
    
    assert args.scanner == "scan" or args.scanner == "update", "need to specify --scanner scan/update"

    if not os.path.exists(args.outdir):
        print("need to specify the result dir")
        return
    

    for f in os.listdir(args.outdir): 
        clean(os.path.join(args.outdir, f), args.scanner)
    
    global AGround, ATP, AFP, FN_LIBs
    print(f"Ground:{AGround}\tTP:{ATP}\tFN:{AGround-ATP}\tFP:{AFP}\n")
    print("\n".join(FN_LIBs))
    
if __name__ == "__main__":
    main()
