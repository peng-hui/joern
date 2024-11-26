import os, subprocess
import argparse, re


# Regular expression to find the lines with 'Result: 8.0' and associated JS file and line numbers
pattern = r"Result: 8\.0.*?:\s([\w\-\.\/]+\.js):(\d+)"
BENCHDIR="/Users/phli/llm4pa/joern/benchmark/"
SCAN_EXT=".scan"
EXPECTED_EXT=".expected"
UPDATE_EXT=".update"


AGround = 0
ATP = 0
AFN = 0
AFP = 0
def clean(redir):
    global AGround, ATP, AFN, AFP
    results = []
    with open(redir, "r") as fp:
        results = [i.strip() for i in fp.readlines()]
        #content = fp.read()
        #matches = re.findall(pattern, content)
        #results = set([f"{match[0]}:{match[1]}".replace("/", ".") for match in matches])
    libname = BENCHDIR + redir.split("/")[-1].removesuffix(UPDATE_EXT) + "_lib"
    print(libname)
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
    with open(redir + ".cleaned", "w") as fp:
        fp.write(f"Ground:{ground}\tScan_TP:{TP}\tScan_FN:{ground-TP}\tScan_FP:{FP}\n")
        fp.write("\n".join(results))
    AGround += ground
    ATP += TP
    AFN += ground-TP
    AFP += FP

def main():
    """Main function. Collect command line arguments and begin"""
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--outdir",
        default="results",
        help=(
            "Output parent directory where the analysis results are saved. "
            "If not set, it would be currentdir/results."
        ),
    )

    args = parser.parse_args()

    if not os.path.exists(args.outdir):
        print("need to specify the result dir")
        return
    for f in os.listdir(args.outdir):
        if f.endswith(UPDATE_EXT):
            clean(os.path.join(args.outdir, f))
    global AGround, ATP, AFN, AFP
    print(f"Ground:{AGround}\tScan_TP:{ATP}\tScan_FN:{AFN}\tScan_FP:{AFP}\n")
    
if __name__ == "__main__":
    main()