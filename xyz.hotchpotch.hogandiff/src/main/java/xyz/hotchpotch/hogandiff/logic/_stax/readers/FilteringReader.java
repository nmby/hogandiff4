package xyz.hotchpotch.hogandiff.logic._stax.readers;

import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

/**
 * 指定された要素を読み飛ばす {@link XMLEventReader} の実装です。<br>
 *
 * @author nmby
 */
public class FilteringReader extends BufferingReader {

    // [static members] ********************************************************

    /**
     * {@link FilteringReader} のビルダーです。<br>
     *
     * @author nmby
     */
    public static class Builder {

        // [static members] ----------------------------------------------------

        // [instance members] --------------------------------------------------

        private final XMLEventReader source;
        private final List<BiPredicate<? super Deque<QName>, ? super StartElement>> filters = new ArrayList<>();

        private Builder(XMLEventReader source) {
            assert source != null;

            this.source = source;
        }

        /**
         * このビルダーにフィルターを追加します。<br>
         * 
         * @param qNames 除外する要素を表すQNameの階層
         * @return このビルダー
         * @throws NullPointerException {@code filter} が {@code null} の場合
         */
        public Builder addFilter(QName... qNames) {
            if (qNames.length == 0) {
                throw new IllegalArgumentException();
            }

            filters.add((currTree, start) -> {
                int i = qNames.length - 1;
                if (!qNames[i].equals(start.getName())) {
                    return false;
                }
                if (currTree.size() + 1 < qNames.length) {
                    return false;
                }

                Iterator<QName> itr = currTree.descendingIterator();
                while (0 < i) {
                    i--;
                    if (!qNames[i].equals(itr.next())) {
                        return false;
                    }
                }
                return true;
            });

            return this;
        }

        /**
         * このビルダーにフィルターを追加します。<br>
         * 
         * @param filter フィルダー
         * @return このビルダー
         * @throws NullPointerException パラメータが {@code null} の場合
         */
        public Builder addFilter(Predicate<? super StartElement> filter) {
            Objects.requireNonNull(filter);

            filters.add((currTree, start) -> filter.test(start));
            return this;
        }

        /**
         * このビルダーにフィルターを追加します。<br>
         * 
         * @param filter フィルダー
         * @return このビルダー
         * @throws NullPointerException パラメータが {@code null} の場合
         */
        public Builder addFilter(BiPredicate<? super Deque<QName>, ? super StartElement> filter) {
            Objects.requireNonNull(filter);

            return this;
        }

        /**
         * このビルダーからリーダーを構成します。<br>
         * 
         * @return 新しいリーダー
         */
        public XMLEventReader build() {
            return new FilteringReader(this);
        }
    }

    /**
     * このクラスのビルダーを返します。<br>
     * 
     * @param source ソースリーダー
     * @return このクラスのビルダー
     * @throws NullPointerException パラメータが {@code null} の場合
     */
    public static Builder builder(XMLEventReader source) {
        Objects.requireNonNull(source);

        return new Builder(source);
    }

    // [instance members] ******************************************************

    private final List<BiPredicate<? super Deque<QName>, ? super StartElement>> filters;

    private FilteringReader(Builder builder) {
        super(builder.source);

        filters = builder.filters;
    }

    /**
     * {@inheritDoc}
     * 
     * この実装は、ソースリーダーの次の要素を調べ、
     * それがスキップ対象の場合は必要な要素が現れるまで不要な要素を読み捨て、
     * 読み込むべき次の要素までリーダーの状態を進めます。<br>
     */
    @Override
    protected void seekNext() throws XMLStreamException {
        while (source.hasNext() && source.peek().isStartElement()) {
            StartElement next = source.peek().asStartElement();
            if (filters.stream().noneMatch(filter -> filter.test(currTree, next))) {
                return;
            }
            int depth = 0;
            do {
                XMLEvent event = source.nextEvent();
                if (event.isStartElement()) {
                    depth++;
                } else if (event.isEndElement()) {
                    depth--;
                }
            } while (source.hasNext() && 0 < depth);
            if (depth != 0) {
                throw new XMLStreamException();
            }
        }
    }
}
