#!/usr/bin/env node

/**
 * 生产环境console清理脚本
 * 检查并替换不必要的console语句
 */

import fs from 'fs'
import path from 'path'
import { fileURLToPath } from 'url'

const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)
const srcDir = path.resolve(__dirname, '../src')

// 需要处理的文件扩展名
const extensions = ['.js', '.vue', '.ts']

// console方法模式
const consolePatterns = [
  /console\.log\(/g,
  /console\.info\(/g,
  /console\.debug\(/g,
  /console\.warn\(/g,
  // 保留 console.error，因为错误日志在生产环境也有用
]

// 检查文件是否包含console语句
function hasConsoleStatements(content) {
  return consolePatterns.some(pattern => pattern.test(content))
}

// 获取所有需要检查的文件
function getAllFiles(dir, fileList = []) {
  const files = fs.readdirSync(dir)

  files.forEach(file => {
    const filePath = path.join(dir, file)
    const stat = fs.statSync(filePath)

    if (stat.isDirectory()) {
      // 跳过node_modules和.git目录
      if (!['node_modules', '.git', 'dist'].includes(file)) {
        getAllFiles(filePath, fileList)
      }
    } else if (extensions.some(ext => file.endsWith(ext))) {
      fileList.push(filePath)
    }
  })

  return fileList
}

// 清理console语句
function cleanConsoleInFile(filePath) {
  try {
    const content = fs.readFileSync(filePath, 'utf8')

    if (!hasConsoleStatements(content)) {
      return { cleaned: false, issues: 0 }
    }

    let cleanedContent = content
    let issues = 0

    // 检查是否已经导入了logger
    const hasLoggerImport = content.includes('import { log } from \'@/utils/logger\'') ||
                           content.includes('import logger from \'@/utils/logger\'')

    if (!hasLoggerImport) {
      console.warn(`⚠️  ${filePath}: 需要导入logger工具`)
    }

    // 记录每个console语句的位置
    const lines = content.split('\n')
    const issuesList = []

    lines.forEach((line, index) => {
      consolePatterns.forEach(pattern => {
        if (pattern.test(line)) {
          issuesList.push({
            line: index + 1,
            content: line.trim(),
            type: pattern.source.replace('\\.', '.').replace('(', '')
          })
          issues++
        }
      })
    })

    if (issuesList.length > 0) {
      console.log(`\n📁 ${path.relative(srcDir, filePath)} (${issues} 个问题):`)
      issuesList.forEach(issue => {
        console.log(`   第${issue.line}行: ${issue.type}`)
        console.log(`   内容: ${issue.content}`)
      })
    }

    // 替换console语句（这里只做报告，不自动替换）
    // cleanedContent = cleanedContent.replace(/console\.(log|info|debug|warn)\(/g, 'log.$1(')

    return { cleaned: false, issues, issuesList }

  } catch (error) {
    console.error(`❌ 处理文件失败 ${filePath}:`, error.message)
    return { cleaned: false, issues: 0, error: error.message }
  }
}

// 主函数
function main() {
  console.log('🔍 开始检查生产环境console语句...\n')

  const files = getAllFiles(srcDir)
  console.log(`📂 找到 ${files.length} 个文件\n`)

  let totalIssues = 0
  let filesWithIssues = 0

  files.forEach(filePath => {
    const result = cleanConsoleInFile(filePath)

    if (result.error) {
      console.error(`❌ ${filePath}: ${result.error}`)
    } else if (result.issues > 0) {
      filesWithIssues++
      totalIssues += result.issues
    }
  })

  console.log('\n' + '='.repeat(60))
  console.log('📊 检查结果汇总:')
  console.log(`   检查文件数: ${files.length}`)
  console.log(`   有问题的文件: ${filesWithIssues}`)
  console.log(`   总问题数: ${totalIssues}`)

  if (totalIssues === 0) {
    console.log('\n✅ 恭喜！没有发现需要清理的console语句')
  } else {
    console.log('\n⚠️  请手动修复上述问题:')
    console.log('   1. 导入logger工具: import { log } from \'@/utils/logger\'')
    console.log('   2. 替换console语句:')
    console.log('      console.log() -> log.info()')
    console.log('      console.debug() -> log.debug()')
    console.log('      console.warn() -> log.warn()')
    console.log('      console.error() -> log.error() (保留)')
    console.log('\n💡 提示: log工具在生产环境会自动禁用调试日志')
  }
}

// 运行脚本
main()