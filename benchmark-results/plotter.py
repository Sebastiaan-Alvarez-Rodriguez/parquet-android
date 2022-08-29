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
import re
import matplotlib.pyplot as plt

# Fontsize used when large plot output is requested.
fontsize_large = 28

class PlotInterface:
    '''Simple interface for plotting implementations.'''
    def plot(self, dataframes, destination, output_type, show, font_large, title=None):
        raise NotImplemented


class LinePlot(PlotInterface):
    '''
    Generates a line plot for given data frames. Expects dataframes of form:
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
            plot_postprocess(plt, ax, fig, destination, output_type, show, font_large, title=title)
        finally:
            plot_reset(plt)


class StackedBarPlot(PlotInterface):
    '''
    Generates stacked barplots, with error whiskers, following https://matplotlib.org/3.1.1/gallery/lines_bars_and_markers/bar_stacked.html. Expects dataframes of form:
    ```
    dataframes=[bar, ...]
    bar=[dataframe,...] (all bars should have equivalent size)
    dataframe={'name': <name>. 'className': <className>, metrics': {'timeNs': {'runs': <data>}}}
    ```
    '''
    def plot(self, dataframes, destination, output_type, show, font_large, bar_labels, segment_labels, title=None, width=None):
        plot_preprocess(plt, font_large)
        try:
            fig, ax = plt.subplots()
            bar_length = len(dataframes[0])
            if not width:
                width = 1.1 / len(dataframes)

            if any(len(bar) != bar_length for bar in dataframes):
                print(f'Error: Found different bar lengths (expected {bar_length}): {[len(bar) for bar in dataframes]}.')
                return

            if len(bar_labels) != len(dataframes):
                print(f'Incorrect amount of labels for bars. Have {len(bar_labels)} labels, {len(dataframes)} bars.')
                return
            if len(segment_labels) != bar_length:
                print(f'Incorrect amount of segment labels for bars. Have {len(segment_labels)} labels, {bar_length} segments per bar.')
                return

            old_averages = np.zeros(len(dataframes))
            accumulated_std_errors = np.zeros(len(dataframes))
            for idx in range(bar_length):
                bar_data = []
                for bar in dataframes:
                    bar_data.append(np.array(bar[idx]['metrics']['timeNs']['runs'])/1000000)

                averages = np.array([dataframe.mean() for dataframe in bar_data])

                accumulated_std_errors = accumulated_std_errors + np.array([np.std(dataframe[np.where((np.percentile(dataframe, 1) <= dataframe) * (dataframe <= np.percentile(dataframe, 99)))]) for dataframe in bar_data])
                errors = accumulated_std_errors if idx+1 == bar_length else None
                ax.bar(list(range(len(averages))), averages, width, bottom=old_averages, yerr=errors, label=segment_labels[idx], capsize=20)
                old_averages = old_averages + averages

            for idx, bar_height in enumerate(old_averages):
                ax.text(idx+width/1.9, bar_height, f"{bar_height:.4f}")

            plt.xticks([idx for idx in range(len(dataframes))], bar_labels)
            ax.grid(axis='y')
            ax.set(xlabel='Data type', ylabel='Execution time (milliseconds)', title=title)
            ax.set_ylim(ymin=0)
            plot_postprocess(plt, ax, fig, destination, output_type, show, font_large, title=title)
        finally:
            plot_reset(plt)


class ScalabilityBarPlot(PlotInterface):
    '''
    Generates subplots for groups of bars. Expects dataframes of form:
    ```
    dataframes=[group, ...]
    group=[bar, ...] (all groups should have equivalent size)
    bar=[dataframe,...] (all bars should have equivalent size)
    dataframe={'name': <name>. 'className': <className>, metrics': {'timeNs': {'runs': <data>}}}
    ```
    '''
    def plot(self, dataframes, destination, output_type, show, font_large, group_labels, bar_labels, segment_labels, title=None, width=None):
        plot_preprocess(plt, font_large)
        try:
            group_length = len(dataframes[0]) # amount of bars in each group
            bar_length = len(dataframes[0][0]) # amount of frames in each bar
            fig, axs = plt.subplots(1, len(dataframes), sharey=True) # every group gets their own plot
            fig.suptitle(title)

            if not width:
                width = 1.1 / len(dataframes)

            if any(len(group) != group_length for group in dataframes):
                print(f'Error: Found different group lengths (expected {group_length}): {[len(group) for group in dataframes]}.')

            for group in dataframes:
                if any(len(bar) != bar_length for bar in group):
                    print(f'Error: Found different bar lengths (expected {bar_length}): {[len(bar) for bar in group]}.')
                    return

            if len(group_labels) != len(dataframes):
                print(f'Incorrect amount of labels for groups. Have {len(group_labels)} labels, {len(dataframes)} groups.')
                return
            
            if len(bar_labels) != group_length:
                print(f'Incorrect amount of labels for groups. Have {len(bar_labels)} labels, {group_length} groups.')
                return
            if len(segment_labels) != bar_length:
                print(f'Incorrect amount of segment labels for bars. Have {len(segment_labels)} labels, {bar_length} segments per bar.')
                return

            print(f'Have {len(dataframes)} groups, {group_length} bars per group, {bar_length} frames per bar')
            for group in dataframes:
                for bar in group:
                    for frame in bar:
                        pass
            # Y coordinate to snap shared y-axis to. 
            ymax = max(max(x['metrics']['timeNs']['runs']) for x in np.array(dataframes).flatten())/1000000*1.1
            for subplot_idx, group in enumerate(dataframes):
                ax = axs[subplot_idx]
                widths = [x for x in range(group_length)]
                old_averages = np.zeros(group_length)
                accumulated_std_errors = np.zeros(group_length)
                for idx in range(bar_length):
                    bar_data = list(np.array(bar[idx]['metrics']['timeNs']['runs'])/1000000 for bar in group)
                    averages = np.array([dataframe.mean() for dataframe in bar_data])

                    accumulated_std_errors = accumulated_std_errors + np.array(
                        [np.std(dataframe[np.where((np.percentile(dataframe, 1) <= dataframe) * (dataframe <= np.percentile(dataframe, 99)))]
                    ) for dataframe in bar_data])
                    errors = accumulated_std_errors if idx+1 == bar_length else None
                    ax.bar(widths, averages, width, bottom=old_averages, yerr=errors, label=segment_labels[idx], capsize=2)
                    old_averages = old_averages + averages

                for location, bar_height in zip(widths, old_averages):
                    ax.text(location, bar_height, f"{bar_height:.0f}", fontsize=fontsize_large*0.8)

                # ax.set_xticks(widths[group_length//2::group_length]+0.001, group_labels)
                ax.set_xticks(widths, bar_labels, rotation=60, fontsize=fontsize_large*0.8)
                ax.tick_params(axis='x', which='minor', direction='out', length=30)
                ax.grid(axis='y')
                # ax.set(xlabel='Data type', ylabel='Execution time (milliseconds)', title=title)
                ax.set_ylim(ymin=0, ymax=ymax)
            plot_postprocess(plt, axs, fig, destination, output_type, show, font_large, loc='upper left', title=title)
        finally:
            plot_reset(plt)


default_generator = 'line'
generators = {
    'line': LinePlot,
    'stackedbar': StackedBarPlot,
    'scalability': ScalabilityBarPlot
}
parametrized_sizes = [1000, 5000, 10000, 15000, 20000]


################################ Data ################################

def identify(data):
    '''
    Identifies results.: Provides benchmark['label'] containing:
     - datatype: File type, e.g. 'csv';
     - iotype: IO operation type, e.g. 'read';
     - datasize: Data size used in operation;
     - compression: Compression on data used in benchmark;
    '''
    for benchmark in data['benchmarks']:
        benchmark['label'] = dict()
        if 'csv' in benchmark['className'].split('.')[-1].lower():
            benchmark['label']['datatype'] = 'csv'
        elif 'parquet' in benchmark['className'].split('.')[-1].lower():
            benchmark['label']['datatype'] = 'parquet'
        else:
            raise RuntimeError(f'Could not identify benchmark datatype: {benchmark}')

        if 'read' in benchmark['name'].lower():
            benchmark['label']['iotype'] = 'read'
        elif 'write' in benchmark['name'].lower():
            benchmark['label']['iotype'] = 'write'
        else:
            raise RuntimeError(f'Could not identify benchmark iotype: {benchmark}')

        if 'snappy' in benchmark['name'].lower():
            benchmark['label']['compression'] = 'snappy'
        else:
            benchmark['label']['compression'] = 'uncompressed'

        match = re.search(r'\[([0-9])\]', benchmark['name'])
        if match:
            benchmark['label']['datasize'] = parametrized_sizes[int(match.group(1))]
        else:
            raise RuntimeError(f'Could not identify benchmark datasize: {benchmark}')


def filter_benchmarks(benchmarks, first=False, datatype=None, iotype=None, datasize=None, compression=None):
    filtered = filter(
        lambda item:
            (not datatype or datatype.lower() == item['label']['datatype']) and
            (not iotype or iotype.lower() == item['label']['iotype']) and
            (not datasize or datasize == item['label']['datasize']) and
            (not compression or compression == item['label']['compression']),
        benchmarks
    )
    return next(filtered) if first else filtered


################################ Utilities ################################

def plot_preprocess(plot, font_large):
    if font_large:
        
        font = {
            'family' : 'DejaVu Sans',
            'size'   : fontsize_large
        }
        plot.rc('font', **font)

def plot_postprocess(plot, axes, fig, destination, output_type, show, font_large, loc='best', title=None):
    axes = axes if isinstance(axes, (list, np.ndarray)) else [axes]
    axes[0].legend(loc=loc, fontsize=18 if font_large else None, frameon=False)

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

    # identify data
    identify(data)

    # Plot data
    ## Plot writes
    if args.generator == 'line':
        for size in parametrized_sizes:
            write = filter_benchmarks(data['benchmarks'], iotype='write', datasize=size)
            read = filter_benchmarks(data['benchmarks'], iotype='read', datasize=size)
            plotinstance.plot(title=f'Write performance ({size} rows)', dataframes=write, destination=args.destination, output_type=args.output_type, show=not args.no_show, font_large=args.font_large)
            plotinstance.plot(title=f'Read performance ({size} rows)', dataframes=read, destination=args.destination, output_type=args.output_type, show=not args.no_show, font_large=args.font_large)
    elif args.generator == 'stackedbar':
        midsize = sorted(parametrized_sizes)[(len(parametrized_sizes)//2)]
        csv = [
            filter_benchmarks(data['benchmarks'], datatype='csv', datasize=midsize, iotype='write', first=True),
            filter_benchmarks(data['benchmarks'], datatype='csv', datasize=midsize, iotype='read', first=True)
        ]
        parquetUncompressed = [
            filter_benchmarks(data['benchmarks'], datatype='parquet', datasize=midsize, compression='uncompressed', iotype='write', first=True),
            filter_benchmarks(data['benchmarks'], datatype='parquet', datasize=midsize, compression='uncompressed', iotype='read', first=True)
        ]
        parquetSnappy = [
            filter_benchmarks(data['benchmarks'], datatype='parquet', datasize=midsize, compression='snappy', iotype='write', first=True),
            filter_benchmarks(data['benchmarks'], datatype='parquet', datasize=midsize, compression='snappy', iotype='read', first=True)
        ]
        plotinstance.plot(
            title=f'Read+Write performance ({midsize} rows)',
            dataframes=[list(csv), list(parquetUncompressed), list(parquetSnappy)],
            bar_labels = ['CSV', 'Parquet Uncompressed', 'Parquet Snappy'],
            segment_labels = ['Write', 'Read'],
            destination=args.destination, 
            output_type=args.output_type, 
            show=not args.no_show,
            font_large=args.font_large,
        )
    elif args.generator == 'scalability':
        dataframes = []
        for size in sorted(parametrized_sizes):
            readframes = sorted(
                list(filter_benchmarks(data['benchmarks'], datasize=size)),
                key=lambda x: x['label']['datatype']+x['label']['compression']+x['label']['iotype']
            )
            bars_csv = list(filter_benchmarks(readframes, datatype='csv'))
            bars_pq_uncompressed = list(filter_benchmarks(readframes, datatype='parquet', compression='uncompressed'))
            bars_pq_snappy = list(filter_benchmarks(readframes, datatype='parquet', compression='snappy'))
            dataframes.append([bars_csv, bars_pq_uncompressed, bars_pq_snappy])
        plotinstance.plot(
            title=f'Read performance',
            dataframes=dataframes,
            group_labels = [str(x) for x in parametrized_sizes],
            bar_labels = ['csv', 'pq', 'pq-snappy'],
            segment_labels = ['Write', 'Read'],
            destination=args.destination, 
            output_type=args.output_type, 
            show=not args.no_show,
            font_large=args.font_large,
        )

if __name__ == '__main__':
    main()