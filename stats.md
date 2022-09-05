# Statistics
This project would not be complete without measuring performance of our implementation.

We executed benchmarks on a Oneplus A5000, also known as a Oneplus 5.
We use the androidx benchmark framework, which ensures:
 - a constant CPU clock speed is used;
 - sources of background noise are blocked during benchmarking;
Additionally, our tests do not contain any random elements (as far as we know).
These measures ensure our test results are reproducible.

We discuss the results below.

## Reproducing
To reproduce the benchmarks accurately, connect a Oneplus A5000 to your machine via USB, clone this project, and execute one or more tests from the `benchmark/` directory using the gradle commands specified in [`benchmark/src/build.gradle`](/benchmark/src/build.gradle).

## Runs
To visualise performance and find out about the composition/variance of the data, we plotted all benchmarks in lineplots.

![Read performance 1000 rows](/benchmark-results/plots/Read_performance_1000_rows.png)
![Read performance 5000 rows](/benchmark-results/plots/Read_performance_5000_rows.png)
![Read performance 10000 rows](/benchmark-results/plots/Read_performance_10000_rows.png)
![Read performance 15000 rows](/benchmark-results/plots/Read_performance_15000_rows.png)
![Read performance 20000 rows](/benchmark-results/plots/Read_performance_20000_rows.png)

![Write performance 1000 rows](/benchmark-results/plots/Write_performance_1000_rows.png)
![Write performance 5000 rows](/benchmark-results/plots/Write_performance_5000_rows.png)
![Write performance 10000 rows](/benchmark-results/plots/Write_performance_10000_rows.png)
![Write performance 15000 rows](/benchmark-results/plots/Write_performance_15000_rows.png)
![Write performance 20000 rows](/benchmark-results/plots/Write_performance_20000_rows.png)

From these plots, we draw the following conclusions:
 1. Using parquet is faster than csv for both reading and writing, with a relatively large speedup factor (at least x10 speed difference).
 2. Writing is generally faster than reading.
 3. Parquet reading and writing is much more stable than csv reading and writing: The variance between measurements is not as large.

These conclusions only hold for our used dataset of course.


## Data Scalability
We created the plot below to compare our tested implementations with ease, and review data scalability, i.e. how much data can be processed in an amount of time and how the software behaves when increasing dataset size.

![Scalability](/benchmark-results/plots/Data_Scalability.png)
*Error bars show standard deviation of read+write performance, 99-percentile.
The numbers on the bars represent the average read+write performance, in milliseconds.*

From this plot, we conclude:
 1. Our tested csv implementation scales linearly rather than constantly with data size increase: Every doubling in dataset size costs more than twice the original processing time.
 2. There is only very little time difference between uncompressed parquet and snappy-compressed parquet.
 3. The parquet implementation scales much further than csv.


## Speedup Factor and Performance Scaling
Here we proof that our parquet (pq) implementation is faster than the tested csv implementation.
Below we plotted the 99%-confidence interval for the speedup factor of our implementation versus csv.

![Read+Write performance on a simple dataset](/benchmark-results/plots/Speedup_factor_of_uncompressed_pq_vs_csv.png)

Simply put, the plot shows '*how many times pq is faster than csv*' for a number of group sizes.
E.g. by looking at this picture at x=10,000, we can state that:
 - 'we are 99% confident that pq is between 12 and 12.5 times faster than csv for 10,000 rows'.
 - 'in our measurements, pq was on average 12.25 times faster than csv for 10,0000 rows'.

We added a trend-formula, and plotted it.
Note that the r-squared value tells how closely the trendline fits our data. An r-squared value of 0.99 (on scale 0.00 to 1.00) should be considered a 'very close fit'.

We conclude that:
 - For the measurements, pq is between 10-16 times faster than csv, with certainty level $\alpha=99$%.
 - pq gets relatively faster than csv when increasing the dataset size. 
 The predicted speedup factor of pq versus csv is given by formula $0.00000001x^2+0.0001x+10.367$, where $x$ is the amount of rows.
 This curve is non-linear, suggesting that using larger dataset sizes will lead to relatively larger speedup factors when comparing with csv. 
 - pq is faster in aspect of both reading and writing.

If you believe we made a statistical computation error, please create a new issue.
