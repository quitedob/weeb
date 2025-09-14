// Constant/emoji/emoji.js

// 使用 @constant 别名来引用同一目录下的 JSON
import emojiText from '@constant/emoji/emojiText.json'
import miyoushe from '@constant/emoji/miyoushe.json'

// 将两个 JSON 文件中的表情整合成一个数组
const emojis = [emojiText]

// 导出给外部组件使用
export default emojis
