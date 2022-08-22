'''
Plot script to generate plots.
Written for python 3.10.4. Theoretically, this works with python 3.8 and up.
Requires the following pips:
  matplotlib
  numpy
'''

import argparse
import json
import numpy as np
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
    dataframe={'name': <name'. 'className': <className>, 'metrics': {'timeNs': {'runs': <data>}}}
    ```
    '''
    def plot(self, dataframes, destination, output_type, show, font_large, title=None):
        plot_preprocess(plt, font_large)
        try:
            fig, ax = plt.subplots()
            for dataframe in dataframes:
                fqn = f'{dataframe["className"].split(".")[-1]}:{dataframe["name"]}'
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
    bar={'label: <label>', 'data': [dataframe,...]} (all bars should have equivalent size)
    dataframe={'name': <name>. 'className': <className>, 'label': <label>, metrics': {'timeNs': {'runs': <data>}}}
    ```
    '''
    def plot(self, dataframes, destination, output_type, show, font_large, title=None, width=None):
        plot_preprocess(plt, font_large)
        try:

            fig, ax = plt.subplots()
            dataframes = list(dataframes)
            bar_length = len(dataframes[0]['data'])
            if not width:
                width = 1.1 / len(dataframes)

            if any(len(bar['data']) != bar_length for bar in dataframes):
                print(f'Error: Found different bar lengths (expected {bar_length}): {[len(bar["data"]) for bar in dataframes]}')
                return

            old_averages = np.zeros(len(dataframes))
            accumulated_std_errors = np.zeros(len(dataframes))
            for idx in range(bar_length):
                bar_data = []
                for bar in dataframes:
                    bar_data.append(np.array(bar['data'][idx]['metrics']['timeNs']['runs'])/1000000)
                print(bar_data)

                averages = np.array([dataframe.mean() for dataframe in bar_data])

                accumulated_std_errors = accumulated_std_errors + np.array([np.std(dataframe[np.where((np.percentile(dataframe, 1) <= dataframe) * (dataframe <= np.percentile(dataframe, 99)))]) for dataframe in bar_data])
                errors = accumulated_std_errors if idx+1 == bar_length else None
                ax.bar(list(range(len(averages))), averages, width, bottom=old_averages, yerr=errors, label=dataframes[0]['data'][idx]['label'], capsize=20)
                old_averages = old_averages + averages

            for idx, bar_height in enumerate(old_averages):
                ax.text(idx+width/1.9, bar_height, f"{bar_height:.4f}")

            plt.xticks([idx for idx in range(len(dataframes))], [bar['label'] for bar in dataframes])
            ax.grid(axis='y')
            ax.set(xlabel='Data type', ylabel='Execution time (milliseconds)', title=title)
            ax.set_ylim(ymin=0)
            plot_postprocess(plt, ax, fig, destination, output_type, show, font_large, title)
        finally:
            plot_reset(plt)



default_generator = 'line'
generators = {
    'line': LinePlot,
    'stackedbar': StackedBarPlot
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
            (not name_contains or name_contains.lower() in item['name'].lower()) and
            (not classname_contains or classname_contains.lower() in item['className'].split('.')[-1].lower()),
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
    plot.savefig(f'{os.path.join(destination, title.replace(" ", "_") if title else "default")}.{output_type}')


################################ Commandline ################################

def add_args(parser):
    parser.add_argument('path', metavar='path', nargs='?', type=str, default='measurements/org.sebastiaan.parquet_android.benchmark.test-benchmarkData.json', help='Result path to read from.')
    parser.add_argument('--generator', metavar='name', type=str, choices=generators.keys(), default=default_generator, help=f'plot generator to execute (default={default_generator}).')
    parser.add_argument('--destination', metavar='path', nargs='?', type=str, default='plots/', help='If set, outputs plot to given output path. Point to a directory.')
    parser.add_argument('--output-type', dest='output_type', metavar='type', type=str, choices=supported_filetypes(), default='pdf', help='Plot output type.')
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
        plotinstance.plot(title='Write performance', dataframes=filter_benchmarks(data['benchmarks'], name_contains='write'), destination=args.destination, output_type=args.output_type, show=not args.no_show, font_large=args.font_large)
        plotinstance.plot(title='Read performance', dataframes=filter_benchmarks(data['benchmarks'], name_contains='read'), destination=args.destination, output_type=args.output_type, show=not args.no_show, font_large=args.font_large)
    elif args.generator == 'stackedbar':
        csvRead = filter_benchmarks(data['benchmarks'], name_contains='read', classname_contains='CSV', first=True)
        csvRead['label'] = 'Read'
        csvWrite = filter_benchmarks(data['benchmarks'], name_contains='write', classname_contains='CSV', first=True)
        csvWrite['label'] = 'Write'
        parquetReadUncompressed = filter_benchmarks(data['benchmarks'], name_contains='readUncompressed', classname_contains='parquet', first=True)
        parquetReadUncompressed['label'] = 'Read'
        parquetWriteUncompressed = filter_benchmarks(data['benchmarks'], name_contains='writeUncompressed', classname_contains='parquet', first=True)
        parquetWriteUncompressed['label'] = 'Write'
        parquetReadSnappy = filter_benchmarks(data['benchmarks'], name_contains='readSnappy', classname_contains='parquet', first=True)
        parquetReadSnappy['label'] = 'Read'
        parquetWriteSnappy = filter_benchmarks(data['benchmarks'], name_contains='writeSnappy', classname_contains='parquet', first=True)
        parquetWriteSnappy['label'] = 'Write'

        plotinstance.plot(
            title='Read+Write performance',
            dataframes=[
                {'label': 'CSV', 'data': [csvRead, csvWrite]}, 
                {'label': 'Parquet Uncompressed', 'data': [parquetReadUncompressed, parquetWriteUncompressed]},
                {'label': 'Parquet Snappy', 'data': [parquetReadSnappy, parquetWriteSnappy]},
            ],
            destination=args.destination, 
            output_type=args.output_type, 
            show=not args.no_show,
            font_large=args.font_large
        )


if __name__ == '__main__':
    main()