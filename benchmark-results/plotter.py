'''
Plot script to generate plots.
Written for python 3.10.4. Theoretically, this works with python 3.8 and up.
Requires the following pips:
  matplotlib
'''

import argparse
import json
import os
import statistics

import matplotlib.pyplot as plt


class PlotInterface:
    '''Simple interface for plotting implementations.'''
    def plot(self, dataframes, destination, output_type, show, font_large, title=None):
        raise NotImplemented


class LinePlot(PlotInterface):
    '''
    Generates a line plot for given data frames. Expectes dataframes of form:
    ```
    dataframes=[dataframe, ...]
    dataframe={"name": <name". "className": <className>, "metrics": {"timeNs": {"runs": <data>}}}
    ```
    '''
    def plot(self, dataframes, destination, output_type, show, font_large, title=None):
        plot_preprocess(plt, font_large)
        try:
            fig, ax = plt.subplots()
            for dataframe in dataframes:
                fqn = dataframe["className"].split('.')[-1]+":"+dataframe["name"]
                runs = [x / 1000000 for x in dataframe["metrics"]["timeNs"]["runs"]]
                ax.plot(runs, label=fqn)

            ax.set(xlabel='Execution number', ylabel='Time (milliseconds)', title=title if title else '')
            ax.set_ylim(ymin=0)
            plot_postprocess(plt, ax, fig, destination, output_type, show, font_large, title)
        finally:
            plot_reset(plt)

class StackedBarPlot(PlotInterface):
    '''
    Generates stacked barplots, with error whiskers, following https://matplotlib.org/3.1.1/gallery/lines_bars_and_markers/bar_stacked.html. Expectes dataframes of form:
    ```
    dataframes=[bar, ...]
    bar=[dataframe,...] (all bars should have equivalent size)
    dataframe={"name": <name". "className": <className>, "metrics": {"timeNs": {"runs": <data>}}}
    ```
    '''
    def plot(self, dataframes, destination, output_type, show, font_large, title=None, width=1):
        plot_preprocess(plt, font_large)
        try:
            fig, ax = plt.subplots()
            dataframes = list(dataframes)
            bar_length = len(dataframes[0])
            if any(len(bar) != bar_length for bar in dataframes):
                print("Error: Found different bar lengths (expected "+str(bar_length)+"): "+str([len(bar) for bar in dataframes]))
                return

            old_bar_data = None

            for idx in range(bar_length):
                bar_data = []
                for bar in dataframes:
                    bar_data.append(bar[idx]["metrics"]["timeNs"]["runs"])

                averages = [statistics.fmean(dataframe) for dataframe in bar_data]
                if not old_bar_data: # first iteration
                    ax.bar(idx, averages, width, label='InitTime')
                    print('bar 1 success')
                else:
                    ax.bar(idx, averages, width, bottom=bar_data, label='InitTime')
                    print('bar '+str(idx+1)+' success')
                old_bar_data = averages
            plot_postprocess(plt, ax, fig, destination, output_type, show, font_large, title)
        finally:
            plot_reset(plt)



default_generator = "line"
generators = {
    "line": LinePlot,
    "stackedbar": StackedBarPlot
}



################################ Utilities ################################
def plot_preprocess(plot, font_large):
    if font_large:
        fontsize = 28
        font = {
            'family' : 'DejaVu Sans',
            'size'   : fontsize
        }
        plot.rc('font', **font)

def plot_postprocess(plot, ax, fig, destination, output_type, show, font_large, title=None):
    if font_large:
        ax.legend(loc='best', fontsize=18, frameon=False)
    else:
        ax.legend(loc='best', frameon=False)

    if font_large:
        fig.set_size_inches(16, 8)

    fig.tight_layout()

    if destination:
       store_plot(destination, plt, title, output_type)

    if font_large:
        plot_reset(plot)

    if show:
        plot.show()

def plot_reset(plot):
    plot.rcdefaults()

def filter_benchmarks(benchmarks, first=False, name_contains=None, classname_contains=None):
    filtered = filter(
        lambda item:
            (not name_contains or name_contains.lower() in item["name"].lower()) and
            (not classname_contains or classname_contains.lower() in item["className"].split('.')[-1].lower()),
        benchmarks
    )
    return next(filtered) if first else filtered

def supported_filetypes():
    '''Returns a `list(str)`, containing the supported filetypes to store for. E.g. ('pdf', 'svg',...). '''
    supported = [x for x in plt.figure().canvas.get_supported_filetypes()]
    plt.clf()
    plt.close()
    return supported

def filetype_is_supported(extension):
    '''Returns `True` iff matplotlib supports filetype, `False` otherwise.'''
    return str(extension).strip().lower() in supported_filetypes()

def store_plot(destination, plot, title, output_type):
    os.makedirs(destination, exist_ok=True)
    plot.savefig(os.path.join(destination, title.replace(' ', '_') if title else 'default')+'.'+output_type)


################################ Commandline ################################

def add_args(parser):
    parser.add_argument('path', metavar='path', nargs='?', type=str, default='measurements/org.sebastiaan.parquet_android.benchmark.test-benchmarkData.json', help='Result path to read from.')
    parser.add_argument('--generator', metavar='name', type=str, choices=generators.keys(), default=default_generator, help='plot generator to execute (default={}).'.format(default_generator))
    parser.add_argument('--destination', metavar='path', nargs='?', type=str, default='plots/', help='If set, outputs plot to given output path. Point to a directory.')
    parser.add_argument('--output-type', dest='output_type', metavar='type', type=str, choices=supported_filetypes(), default="pdf", help='Plot output type.')
    parser.add_argument('--no-show', dest='no_show', help='Do not show generated plot (useful on servers without window managers/forwarding).', action='store_true')
    parser.add_argument('--font-large', dest='font_large', help='If set, generates plots with larger font.', action='store_true')


def main():
    parser = argparse.ArgumentParser(
        prog='plotter',
        formatter_class=argparse.RawTextHelpFormatter,
        description='Generate plots from results.'
    )
    retval = True
    add_args(parser)

    args = parser.parse_args()

    # Prepare plot generator
    plotclass = generators[args.generator]
    plotinstance = plotclass()

    # Read data
    with open(args.path, 'r') as file:
        data = json.load(file)

    # Plot data
    ## Plot writes
    if args.generator == 'line':
        plotinstance.plot(title='Write performance', dataframes=filter_benchmarks(data["benchmarks"], name_contains="write"), destination=args.destination, output_type=args.output_type, show=not args.no_show, font_large=args.font_large)
        plotinstance.plot(title='Read performance', dataframes=filter_benchmarks(data["benchmarks"], name_contains="read"), destination=args.destination, output_type=args.output_type, show=not args.no_show, font_large=args.font_large)
    elif args.generator == 'stackedbar':
        csvRead = filter_benchmarks(data["benchmarks"], name_contains="read", classname_contains="CSV", first=True)
        csvWrite = filter_benchmarks(data["benchmarks"], name_contains="write", classname_contains="CSV", first=True)
        parquetReadUncompressed = filter_benchmarks(data["benchmarks"], name_contains="readUncompressed", classname_contains="parquet", first=True)
        parquetWriteUncompressed = filter_benchmarks(data["benchmarks"], name_contains="writeUncompressed", classname_contains="parquet", first=True)
        parquetReadSnappy = filter_benchmarks(data["benchmarks"], name_contains="readSnappy", classname_contains="parquet", first=True)
        parquetWriteSnappy = filter_benchmarks(data["benchmarks"], name_contains="writeSnappy", classname_contains="parquet", first=True)
        plotinstance.plot(title='Write performance', dataframes=[[csvRead, csvWrite], [parquetReadUncompressed, parquetWriteUncompressed], [parquetReadSnappy, parquetWriteSnappy]], destination=args.destination, output_type=args.output_type, show=not args.no_show, font_large=args.font_large)


if __name__ == '__main__':
    main()