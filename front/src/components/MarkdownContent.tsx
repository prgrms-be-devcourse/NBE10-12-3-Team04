import type { ReactNode } from 'react';

type MarkdownContentProps = {
  markdown: string;
  variant?: 'editor' | 'detail';
  className?: string;
};

function renderInlineMarkdown(text: string) {
  return text.split(/(\*\*[^*]+\*\*)/g).map((part, index) => {
    if (part.startsWith('**') && part.endsWith('**')) {
      return <strong key={`${part}-${index}`}>{part.slice(2, -2)}</strong>;
    }
    return part;
  });
}

export default function MarkdownContent({ markdown, variant = 'editor', className = '' }: MarkdownContentProps) {
  const lines = markdown.split('\n');
  const blocks: ReactNode[] = [];
  let listItems: string[] = [];
  const compact = variant === 'detail';

  const flushList = () => {
    if (!listItems.length) return;
    blocks.push(
      <ul key={`list-${blocks.length}`} className={`${compact ? 'my-2 text-sm' : 'my-3 text-base'} list-disc space-y-1 pl-5 text-gray-700`}>
        {listItems.map((item, index) => (
          <li key={`${item}-${index}`}>{renderInlineMarkdown(item)}</li>
        ))}
      </ul>,
    );
    listItems = [];
  };

  lines.forEach((line, index) => {
    const trimmed = line.trim();
    if (!trimmed) {
      flushList();
      blocks.push(<div key={`space-${index}`} className={compact ? 'h-2' : 'h-3'} />);
      return;
    }
    if (trimmed.startsWith('- ')) {
      listItems.push(trimmed.slice(2));
      return;
    }

    flushList();
    const heading = trimmed.match(/^(#{1,6})\s+(.+)$/);
    if (heading) {
      const level = heading[1].length;
      const headingText = renderInlineMarkdown(heading[2]);
      const headingClass = compact
        ? ['mt-4 text-lg', 'mt-3 text-base', 'mt-3 text-sm', 'mt-2 text-sm', 'mt-2 text-xs', 'mt-2 text-xs'][level - 1]
        : ['mt-6 text-2xl', 'mt-6 text-xl', 'mt-5 text-lg', 'mt-4 text-base', 'mt-3 text-sm', 'mt-3 text-xs'][level - 1];

      blocks.push(
        <div key={index} role="heading" aria-level={level} className={`${headingClass} font-bold text-gray-900`}>
          {headingText}
        </div>,
      );
      return;
    }

    blocks.push(<p key={index} className={`${compact ? 'text-sm leading-6' : 'text-base leading-7'} text-gray-700`}>{renderInlineMarkdown(trimmed)}</p>);
  });

  flushList();
  return <div className={className}>{blocks}</div>;
}
