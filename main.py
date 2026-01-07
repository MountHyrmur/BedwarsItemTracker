import sys
import polars as pl
import matplotlib.pyplot as plt
from matplotlib.ticker import MultipleLocator


GROUP_WINDOW_MS = 100


# Merge many item spawns within the GROUP_WINDOW_MS time window into one spawn
def merge_close_item_spawns(df):
    df = df.with_columns(
        (
            (pl.col("timeMs") - pl.col("timeMs").shift(1) > GROUP_WINDOW_MS)
            | (pl.col("item") != pl.col("item").shift(1))
        )
        .fill_null(True)
        .cum_sum()
        .alias("group")
    )

    print(df)

    df = df.group_by("group", maintain_order=True).agg(
        [
            pl.col("item").first(),
            pl.col("count").sum(),
            pl.col("timeMs").first(),
        ]
    )

    return df.drop("group")


def main():
    path = sys.argv[1]
    merge = len(sys.argv) > 2 and sys.argv[2].lower() == "true"

    df = pl.read_csv(path)

    if merge:
        df = merge_close_item_spawns(df)

    df = df.with_columns((pl.col("timeMs") / 1000).alias("timeS"))

    for group in df.group_by("item", maintain_order=True):
        (group_data, frame) = group
        category = group_data[0]
        frame = frame.with_columns(
            (pl.col("timeS").diff().alias("timeDiff"),)
        ).drop_nulls("timeDiff")
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
