import matplotlib.pyplot as plt
import pandas as pd


def main(filename: str) -> None:
    df = pd.read_csv(filename, sep=',')

    # funkcje agregujące
    agg_functions = {'interface': 'first', 'packets_in_s': 'mean', 'packets_in': 'sum'}

    # grupuj po czasie i interfejsie, a następnie zaaplikuj zdefiniowane funkcje
    cleaned_data = df.groupby(['timestamp', 'interface']).aggregate(agg_functions)

    # interfejsy do odfiltrowania
    bad_iface = ['lo', 'eth0', 'total']

    # interfejsy, które chcemy wyświwetlić
    iface = [ x for x in cleaned_data['interface'].unique().tolist() if x not in bad_iface]

    # rysowanie wykresów
    for x in iface:
        tmp = cleaned_data.loc[cleaned_data['interface'] == x]['packets_in'].to_list()
        plt.plot([ i for i in range(len(tmp)) ], tmp)

    plt.yscale('log')
    plt.xlabel('sekunda pomiaru')
    plt.ylabel('ilość pakietów wejściowych')
    plt.legend(iface)
    # plt.show()
    plt.savefig('result.png', dpi=300)


if __name__ == '__main__':
    main('tmp.txt')

