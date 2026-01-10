import sys
import polars as pl
import numpy as np
import matplotlib.pyplot as plt
from matplotlib.ticker import MultipleLocator
from sklearn.cluster import HDBSCAN


MAX_TIMES = {"iron_ingot": 4.0, "gold_ingot": 15.0}
EPSILONS = {"iron_ingot": 0.1, "gold_ingot": 2.0}
MIN_CLUSTER_SIZES = {"iron_ingot": 15, "gold_ingot": 3}

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

    df = df.group_by("group", maintain_order=True).agg(
        [
            pl.col("item").first(),
            pl.col("count").sum(),
            pl.col("timeMs").first(),
        ]
    )

    return df.drop("group")


def filter_big_outliers(df):
    group = df["item"][0]
    max_diff = MAX_TIMES.get(group)
    if max_diff is None:
        return df

    return df.filter(pl.col("timeDiff") <= max_diff)


def main():
    path = sys.argv[1]
    merge = len(sys.argv) <= 2 or sys.argv[2].lower() == "true"

    df = pl.read_csv(path)

    if merge:
        df = merge_close_item_spawns(df)

    df = df.with_columns((pl.col("timeMs") / 1000).alias("timeS"))

    groups = list(df.group_by("item", maintain_order=True))

    for group in groups:
        (group_data, frame) = group
        category = group_data[0]
        frame = frame.with_columns(
            (
                (pl.col("timeS").diff().alias("timeDiff")),
                (pl.col("count") * 5 + 25).alias("dotSize"),
            )
        ).drop_nulls("timeDiff")
        frame = filter_big_outliers(frame)

        hdb = HDBSCAN(
            copy=True,
            min_cluster_size=MIN_CLUSTER_SIZES.get(category),
            min_samples=5,
            cluster_selection_method="eom",
        )

        hdb.fit(frame["timeDiff"].to_numpy().reshape(-1, 1))
        labels = hdb.dbscan_clustering(
            cut_distance=EPSILONS.get(category),
            min_cluster_size=MIN_CLUSTER_SIZES.get(category),
        )

        frame = frame.with_columns(pl.Series("cluster", labels))

        plt.figure()

        label_set = set(labels)

        colors = plt.cm.tab10(np.linspace(0, 1, len(label_set)))

        print(f"Average time between spawns for {category}:")
        for cluster, color in zip(label_set, colors):
            if cluster == -1:
                continue
            filtered_frame = frame.filter(pl.col("cluster") == cluster)

            average = filtered_frame["timeDiff"].mean()

            label = f"Cluster {cluster}"

            print(f"{label}: {average:.2f}s")

            plt.scatter(
                filtered_frame["timeS"],
                filtered_frame["timeDiff"],
                s=filtered_frame["dotSize"],
                c=[color],
                label=label,
            )

        plt.legend()
        plt.title(f"Time between item spawns - {category}")
        plt.xlabel("Time from game start [sec]")
        plt.ylabel("Delay between two items spawned [sec]")
        plt.gca().xaxis.set_major_locator(MultipleLocator(10))
        plt.gca().xaxis.set_minor_locator(MultipleLocator(2))
        plt.gca().yaxis.set_major_locator(MultipleLocator(0.5))
        plt.gca().yaxis.set_minor_locator(MultipleLocator(0.1))
        plt.grid(which="minor", alpha=0.1)
        plt.grid(which="major", alpha=0.2)
    plt.show()


if __name__ == "__main__":
    main()
