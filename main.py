import sys
import csv
import polars as pl
import matplotlib.pyplot as plt
from matplotlib.ticker import MultipleLocator


def main():
    path = sys.argv[1]

    df = pl.read_csv(path)

    df = df.with_columns((pl.col("timeMs") / 1000).alias("timeS"))

    # print(df)

    # data = []
    # with open(path, newline="") as csvfile:
    #     reader = csv.reader(csvfile)
    #     data = list(reader)[1:]

    # times = []
    # counts = []

    # for row in data:
    #     times.append(float(row[0]) / 1000)
    #     counts.append(int(row[1]))

    # timeInSecs = [round(x, 2) for x in times[1:]]
    # timeDifferences = []

    # for i in range(1, len(times)):
    #     timeDifferences.append(times[i] - times[i - 1])
    for group in df.group_by("item"):
        (group_data, frame) = group
        category = group_data[0]
        frame = frame.with_columns(
            (
                pl.col("timeS").diff().alias("timeDiff"),
                pl.col("timeMs").diff().alias("timeDiffDebug"),
            )
        ).drop_nulls("timeDiff")
        if category == "iron_ingot":
            frame.write_csv("debug.csv")
        plt.plot(frame["timeS"].to_list(), frame["timeDiff"].to_list(), label=category)

    plt.legend()
    plt.title("Time between item spawnes")
    plt.xlabel("Time from game start [sec]")
    plt.ylabel("Delay between two items spawned up [sec]")
    plt.gca().xaxis.set_major_locator(MultipleLocator(10))
    plt.gca().xaxis.set_minor_locator(MultipleLocator(2))
    plt.gca().yaxis.set_major_locator(MultipleLocator(0.5))
    plt.gca().yaxis.set_minor_locator(MultipleLocator(0.1))
    plt.grid(which="minor", alpha=0.1)
    plt.grid(which="major", alpha=0.2)
    plt.show()


if __name__ == "__main__":
    main()
