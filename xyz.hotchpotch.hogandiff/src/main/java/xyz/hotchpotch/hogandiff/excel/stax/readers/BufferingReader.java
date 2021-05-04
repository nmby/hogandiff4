package xyz.hotchpotch.hogandiff.excel.stax.readers;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.NoSuchElementException;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

/**
 * {@link XMLEventReader} の汎用的な抽象実装です。<br>
 * このリーダーは、ソースリーダーに加えて内部バッファを有しており、
 * 内部バッファに格納されたイベントを優先的にクライアントに返します。<br>
 * このクラスのサブクラスは、内部バッファに新たな要素イベントを追加することにより、
 * ソースリーダーには無い要素を追加することができます。<br>
 *
 * @author nmby
 */
public abstract class BufferingReader implements XMLEventReader {
    
    // [static members] ********************************************************
    
    // [instance members] ******************************************************
    
    /** ソースリーダー */
    protected final XMLEventReader source;
    
    /** 次のイベントを格納するバッファ */
    protected final Deque<XMLEvent> buffer = new ArrayDeque<>();
    
    /** 現在の要素ツリー */
    protected final Deque<QName> currTree = new ArrayDeque<>();
    
    private boolean isReady;
    
    /**
     * 新しいリーダーを生成します。<br>
     * 
     * @param source ソースリーダー
     */
    protected BufferingReader(XMLEventReader source) {
        assert source != null;
        
        this.source = source;
    }
    
    /**
     * このリーダーの現在の状態で {@link #hasNext()}, {@link #peek()}, {@link #nextEvent()}, {@link #next()}
     * が初めて呼ばれた際に実行されます。<br>
     * このリーダーの状態が変わらない間に上記メソッドが再び呼ばれた場合は、
     * このメソッドは実行されません。<br>
     * <br>
     * このメソッドの実装者は、ソースリーダー {@link #source} の次のイベントを調べ、
     * 次のいずれかの措置をとることができます。<br>
     * <br>
     * <strong>A) ソースリーダーの次のイベントをこのリーダーの次のイベントとしてそのまま利用したい場合</strong><br>
     * 何もせずにこのメソッドの処理を終えます。
     * こうすることにより、ソースリーダーの次のイベントがそのまま
     * このリーダーの次のイベントとなります。<br>
     * <br>
     * <strong>B) ソースリーダーの次のイベントが不要であり読み飛ばしたい場合</strong><br>
     * 必要な要素が現れるまで、ソースリーダーを空読みします。<br>
     * <br>
     * <strong>C) ソースリーダーの次のイベントの前に別のイベントを追加したい場合</strong><br>
     * {@link #buffer} に自身で生成したイベントを追加します。
     * 加えて、ソースリーダーの次のイベントを取り出し、{@link #buffer} の末尾に追加します
     * （これを怠ると無限ループに陥るため注意してください）。<br>
     * こうすることにより、まず {@link #buffer} 内のイベントが消費され、
     * その後にソースリーダーのイベントが消費されるようになります。<br>
     * 
     * @throws XMLStreamException XMLイベントの解析に失敗した場合
     */
    protected abstract void seekNext() throws XMLStreamException;
    
    /**
     * {@inheritDoc}
     * <br>
     * この実装は、{@link #buffer} か {@link #source} のいずれかに
     * 次のイベントが存在する場合に {@code true} を返します。<br>
     */
    @Override
    public boolean hasNext() {
        if (!isReady && buffer.isEmpty()) {
            try {
                seekNext();
            } catch (XMLStreamException e) {
                throw new RuntimeException(e);
            }
            isReady = true;
        }
        return !buffer.isEmpty() || source.hasNext();
    }
    
    /**
     * {@inheritDoc}
     * <br>
     * この実装は、{@link #buffer} が空でない場合は {@link #buffer} の先頭のイベントを、
     * {@link #buffer} が空であり {@link #source} が空でない場合は
     * {@link #source} の先頭のイベントを返します。<br>
     * どちらも空の場合は {@code null} を返します。<br>
     */
    @Override
    public XMLEvent peek() throws XMLStreamException {
        if (!isReady && buffer.isEmpty()) {
            seekNext();
            isReady = true;
        }
        return buffer.isEmpty()
                ? source.peek()
                : buffer.peek();
    }
    
    /**
     * {@inheritDoc}
     * <br>
     * この実装は、{@link #buffer} が空でない場合は {@link #buffer} の先頭のイベントを、
     * {@link #buffer} が空であり {@link #source} が空でない場合は
     * {@link #source} の先頭のイベントを返します。<br>
     * どちらも空の場合は例外をスローします。<br>
     * 
     * @throws NoSuchElementException 次の要素が存在しない場合
     */
    @Override
    public XMLEvent nextEvent() throws XMLStreamException {
        if (!isReady && buffer.isEmpty()) {
            seekNext();
        }
        XMLEvent next = buffer.isEmpty()
                ? source.nextEvent()
                : buffer.remove();
        
        if (next.isStartElement()) {
            currTree.addLast(next.asStartElement().getName());
        } else if (next.isEndElement()) {
            currTree.removeLast();
        }
        isReady = false;
        return next;
    }
    
    /**
     * {@link #nextEvent()} と同じ。<br>
     * 但し、チェック例外は {@link RuntimeException} にラップしてスローします。<br>
     */
    @Override
    public Object next() {
        try {
            return nextEvent();
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * ソースリーダーをクローズします。<br>
     */
    @Override
    public void close() throws XMLStreamException {
        source.close();
    }
    
    /**
     * このオペレーションはサポートされません。<br>
     * 
     * @throws UnsupportedOperationException このオペレーションを実行した場合
     */
    @Override
    public String getElementText() throws XMLStreamException {
        throw new UnsupportedOperationException();
    }
    
    /**
     * このオペレーションはサポートされません。<br>
     * 
     * @throws UnsupportedOperationException このオペレーションを実行した場合
     */
    @Override
    public XMLEvent nextTag() throws XMLStreamException {
        throw new UnsupportedOperationException();
    }
    
    /**
     * このオペレーションはサポートされません。<br>
     * 
     * @throws UnsupportedOperationException このオペレーションを実行した場合
     */
    @Override
    public Object getProperty(String name) throws IllegalArgumentException {
        throw new UnsupportedOperationException();
    }
}
